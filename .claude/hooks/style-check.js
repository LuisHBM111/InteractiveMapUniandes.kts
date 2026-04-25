#!/usr/bin/env node
// Project style hook: warns Claude when an edit drifts from the agreed style.
// Reads PostToolUse JSON from stdin, scans the file path it touched, prints a
// note to stderr (Claude sees it). Never exits non-zero — it's advisory only.

const fs = require('fs');

let payload = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (c) => (payload += c));
process.stdin.on('end', () => {
  let event;
  try {
    event = JSON.parse(payload || '{}');
  } catch {
    process.exit(0);
  }
  const filePath = event?.tool_input?.file_path;
  if (!filePath || !/\.(kt|java|xml)$/.test(filePath)) process.exit(0);
  if (!fs.existsSync(filePath)) process.exit(0);

  const text = fs.readFileSync(filePath, 'utf8');
  const warnings = [];

  // Multi-line KDoc / JavaDoc blocks are off-style for this repo
  if (/\/\*\*[\s\S]*?\*\//m.test(text)) {
    warnings.push('multi-line /** ... */ docs found — replace with short inline // comments at end of line');
  }

  // TODO / FIXME / XXX are banned by CLAUDE.md
  const todoMatch = text.match(/\/\/\s*(TODO|FIXME|XXX)\b/);
  if (todoMatch) warnings.push(`stray ${todoMatch[1]} comment — drop it or just do the work`);

  // Files growing too big
  const lines = text.split('\n').length;
  if (lines > 800) warnings.push(`file is ${lines} lines (> 800) — consider splitting`);

  // Comment density on Kotlin/Java
  if (/\.(kt|java)$/.test(filePath)) {
    const commentLines = text.split('\n').filter((l) => /^\s*\/\//.test(l)).length;
    if (commentLines > Math.max(20, lines * 0.25)) {
      warnings.push(`${commentLines} comment lines — too many; keep them inline at end of code lines`);
    }
  }

  if (warnings.length === 0) process.exit(0);
  process.stderr.write('[style-check] ' + filePath + '\n');
  warnings.forEach((w) => process.stderr.write('  - ' + w + '\n'));
  process.exit(0);
});
