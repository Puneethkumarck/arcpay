#!/usr/bin/env bash
# Publish the pages in wiki/ to the GitHub project wiki.
# One-time prerequisite: the wiki repo must exist — open the repo's "Wiki" tab and
# create the first page once (any content + Save). That materializes <repo>.wiki.git.
# Then run:  ./wiki/publish.sh   (from the repo root)
set -euo pipefail
REPO="${1:-Puneethkumarck/arcpay}"
SRC="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"
echo "Cloning ${REPO}.wiki.git ..."
git clone "https://github.com/${REPO}.wiki.git" "$TMP"
cp "$SRC"/*.md "$TMP"/                 # all wiki pages + _Sidebar.md (publish.sh is .sh, not copied)
( cd "$TMP" && git add -A && git -c user.name="$(git config user.name)" -c user.email="$(git config user.email)" commit -m "docs: sync ArcPay engineering wiki" && git push )
rm -rf "$TMP"
echo "Published → https://github.com/${REPO}/wiki"
