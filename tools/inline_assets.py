#!/usr/bin/env python3
"""
Simple helper to inline JS/CSS/fonts/images into the HTML template.
Usage:
  python tools/inline_assets.py --input-dir app/src/main/assets/inline --output-dir app/src/main/assets/inline_out

This script will scan the `js`, `fonts`, `images` subfolders and produce a JSON file containing base64-encoded resources and small snippets to inject into templates.
"""
import os
import argparse
import base64
import json


def encode_file(path):
    with open(path, 'rb') as f:
        data = f.read()
    return base64.b64encode(data).decode('ascii')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--input-dir', default='app/src/main/assets/inline')
    parser.add_argument('--output-dir', default='app/src/main/assets/inline_out')
    args = parser.parse_args()

    out = {'js': {}, 'fonts': {}, 'images': {}}

    for kind in ['js', 'fonts', 'images']:
        src_dir = os.path.join(args.input_dir, kind)
        if not os.path.exists(src_dir):
            continue
        for fname in os.listdir(src_dir):
            if fname.startswith('.'):
                continue
            path = os.path.join(src_dir, fname)
            key = fname
            out[kind][key] = encode_file(path)

    os.makedirs(args.output_dir, exist_ok=True)
    with open(os.path.join(args.output_dir, 'inlined_assets.json'), 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
    print('Wrote', os.path.join(args.output_dir, 'inlined_assets.json'))


if __name__ == '__main__':
    main()

