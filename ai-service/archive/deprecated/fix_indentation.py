#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Fix indentation in simple_vietnamese_nlp.py"""

import re

with open('simple_vietnamese_nlp.py', 'r', encoding='utf-8') as f:
    content = f.read()

# Split into lines
lines = content.split('\n')

# Track indentation level
fixed_lines = []
in_class = False
in_method = False
expected_indent = 0

for i, line in enumerate(lines):
    stripped = line.lstrip()
    
    # Detect class definition
    if stripped.startswith('class '):
        in_class = True
        expected_indent = 0
        fixed_lines.append(line)
        continue
    
    # Detect method definition (in class)
    if in_class and stripped.startswith('def '):
        in_method = True
        expected_indent = 4  # Methods get 4 spaces
        fixed_lines.append(' ' * 4 + stripped)
        continue
    
    # Empty lines
    if not stripped:
        fixed_lines.append('')
        continue
    
    # Lines outside class
    if not in_class:
        fixed_lines.append(line)
        continue
    
    # Lines inside method
    if in_method:
        # Check if this line is a new method (next method starting)
        if stripped.startswith('def '):
            expected_indent = 4
            fixed_lines.append(' ' * 4 + stripped)
            continue
        
        # Method body should be indented 8 spaces
        # But nested blocks (if, for, try, etc.) need more
        
        # Count original indentation to detect nesting
        original_indent = len(line) - len(stripped)
        
        # Determine proper indentation based on line content
        if stripped.startswith('"""') or stripped.startswith("'''"):
            # Docstrings in methods
            indent = 8
        elif any(stripped.startswith(kw) for kw in ['if ', 'for ', 'while ', 'try:', 'except', 'with ', 'elif ', 'else:']):
            # Control structures
            indent = 8
        elif line.lstrip().startswith('# '):
            # Comments
            indent = 8
        elif original_indent > 4:
            # Nested content - preserve relative indentation
            # Map old indentation to new: 4->8, 8->12, 12->16, etc.
            relative_indent = max(0, original_indent - 4)
            indent = 8 + relative_indent
        else:
            # Regular statements
            indent = 8
        
        fixed_lines.append(' ' * indent + stripped)
    else:
        # Class-level attributes (not in a method yet)
        fixed_lines.append(' ' * 4 + stripped)

# Write fixed content
with open('simple_vietnamese_nlp.py', 'w', encoding='utf-8') as f:
    f.write('\n'.join(fixed_lines))

print("✓ Fixed indentation")
