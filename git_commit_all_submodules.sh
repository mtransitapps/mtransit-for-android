#!/bin/bash
# ORIGINAL FILE: https://github.com/mtransitapps/commons/tree/master/shared-overwrite

mapfile -t SUBMODULES < <(git submodule foreach --quiet 'basename "$(pwd)"');
echo "${#SUBMODULES[@]} submodule(s): ";

COMMIT_MESSAGE_TITLE_START="Sync code";
if [[ -n "${CI}" ]]; then
  COMMIT_MESSAGE_TITLE_START="CI: $COMMIT_MESSAGE_TITLE_START";
fi

COMMIT_MESSAGE_TITLE="";
COMMIT_MESSAGE_BODY="";
ONE_COMMIT_MESSAGE="";
COMMITS=0;
for SUBMODULE in "${SUBMODULES[@]}"; do
  SUBMODULE_CURRENT_SHA=$(git ls-tree HEAD "$SUBMODULE" | awk '{ print $3 }')
  SUBMODULE_LATEST_SHA=$(git -C "$SUBMODULE" rev-parse HEAD);
  if [[ "$SUBMODULE_CURRENT_SHA" == "$SUBMODULE_LATEST_SHA" ]]; then
    echo "- '$SUBMODULE': no change.";
  else
    mapfile -t SUBMODULE_CHANGES < <(git -C "$SUBMODULE" log --oneline --pretty=format:"%s" "$SUBMODULE_CURRENT_SHA"..HEAD);
    echo "- '$SUBMODULE': ${#SUBMODULE_CHANGES[@]} changes: ";
    for SUBMODULE_CHANGE in "${SUBMODULE_CHANGES[@]}"; do
      echo "  - '$SUBMODULE_CHANGE'";
      COMMIT_MESSAGE_BODY+="\n- $SUBMODULE: $SUBMODULE_CHANGE";
      ONE_COMMIT_MESSAGE="$COMMIT_MESSAGE_TITLE_START from '$SUBMODULE': $SUBMODULE_CHANGE";
      ((COMMITS++));
    done
    if [[ -z $COMMIT_MESSAGE_TITLE ]]; then
      COMMIT_MESSAGE_TITLE+="$COMMIT_MESSAGE_TITLE_START from '$SUBMODULE'";
    else
      COMMIT_MESSAGE_TITLE+=" & '$SUBMODULE'";
    fi
    git add "$SUBMODULE";
  fi
done

echo "$COMMITS change(s) to commit.";

if [[ "${COMMITS}" -eq 1 ]]; then
  echo "--------------------------------------------------------------------------------";
  echo -e "$ONE_COMMIT_MESSAGE";
  echo "--------------------------------------------------------------------------------";
  printf "$ONE_COMMIT_MESSAGE" | git commit -F -
elif [[ "${COMMITS}" -gt 1 ]]; then
  COMMIT_MESSAGE="$COMMIT_MESSAGE_TITLE:\n$COMMIT_MESSAGE_BODY";
  echo "--------------------------------------------------------------------------------";
  echo -e "$COMMIT_MESSAGE";
  echo "--------------------------------------------------------------------------------";
  printf "$COMMIT_MESSAGE" | git commit -F -
# else # 0 commits
# do nothing
fi
