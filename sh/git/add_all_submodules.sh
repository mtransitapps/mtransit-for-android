# ORIGINAL FILE: https://github.com/mtransitapps/commons/tree/master/shared-overwrite
SCRIPT_DIR="$(dirname "$0")";
ROOT_DIR="$(git rev-parse --show-toplevel)";

git -C "$ROOT_DIR" submodule foreach 'echo "> Staging $sm_path..." && git -C ../ add $sm_path && echo "> Staging $sm_path... DONE"';
