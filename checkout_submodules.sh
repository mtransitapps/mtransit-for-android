#!/bin/bash
# ORIGINAL FILE: https://github.com/mtransitapps/commons/tree/master/shared-overwrite
# DEPRECATED: use submodules_init.sh
SCRIPT_DIR="$(dirname "$0")";
exec "${SCRIPT_DIR}/submodules_init.sh" "$@";
