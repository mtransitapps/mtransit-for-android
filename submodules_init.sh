#!/bin/bash
# ORIGINAL FILE: https://github.com/mtransitapps/commons/tree/master/shared-overwrite

# DEPRECATED: use sh/git/submodules_init.sh
# TODO: remove after update mtransitapps/gh-actions/.github/actions/setup)

SCRIPT_DIR="$(dirname "$0")";
exec "${SCRIPT_DIR}/sh/git/submodules_init.sh" "$@";
