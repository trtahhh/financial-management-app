#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Rebuild simple_vietnamese_nlp.py with correct indentation
Reads the broken file and fixes all indentation issues
"""

def fix_indentation():
    with open('simple_vietnamese_nlp.py', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    fixed_lines = []
    inside_class = False
    inside_method = False
    inside_dict = False
    dict_depth = 0
    
    for i, line in enumerate(lines):
        # Get original content
        stripped = line.strip()
        
        # Empty lines
        if not stripped:
            fixed_lines.append('')
            continue
        
        # Class definition
        if stripped.startswith('class '):
            inside_class = True
            inside_method = False
            fixed_lines.append(line.rstrip())
            continue
        
        # Outside class - keep as is
        if not inside_class:
            fixed_lines.append(line.rstrip())
            continue
        
        # Method definition (4 spaces)
        if stripped.startswith('def '):
            inside_method = True
            fixed_lines.append('    ' + stripped)
            continue
        
        # Inside method - all content gets 8 spaces base
        if inside_method:
            # Track dictionary/list depth for proper nesting
            open_brackets = stripped.count('{') + stripped.count('[')
            close_brackets = stripped.count('}') + stripped.count(']')
            
            # Determine indentation level
            if stripped.startswith('"""') or stripped.startswith("'''"):
                indent = 8
            elif stripped.startswith('#'):
                indent = 8
            elif stripped in ['}', '})', '}', '],', ']']:
                dict_depth = max(0, dict_depth - 1)
                indent = 8 + (dict_depth * 4)
            elif any(stripped.startswith(x) for x in ["'", '"']) and ':' not in stripped:
                # List items
                indent = 12 + (dict_depth * 4)
            elif stripped.startswith('r\'') or stripped.startswith('r"'):
                # Regex patterns
                indent = 12 + (dict_depth * 4)
            elif ':' in stripped and '{' in stripped:
                # Dict key with opening
                indent = 8 + (dict_depth * 4)
                dict_depth += 1
            elif ':' in stripped and ('{' in line[:line.index(stripped)] if stripped in line else False):
                # Dict values
                indent = 8 + (dict_depth * 4)
            elif any(stripped.startswith(kw) for kw in ['if ', 'for ', 'while ', 'try:', 'except', 'with ', 'elif ', 'else:', 'return ']):
                # Control flow
                indent = 8
            elif '=' in stripped and '==' not in stripped:
                # Assignments
                indent = 8
            else:
                # Default method body
                indent = 8
            
            # Adjust for closing brackets
            if close_brackets > open_brackets:
                dict_depth = max(0, dict_depth - (close_brackets - open_brackets))
            
            fixed_lines.append(' ' * indent + stripped)
        else:
            # Class-level attributes (rare in this file)
            fixed_lines.append('    ' + stripped)
    
    # Write fixed file
    with open('simple_vietnamese_nlp.py', 'w', encoding='utf-8') as f:
        f.write('\n'.join(fixed_lines))
    
    print("✓ File rebuilt with correct indentation")

if __name__ == '__main__':
    fix_indentation()
