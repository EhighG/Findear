#!/bin/bash

GIT_ROOT_DIRECTORY=$(git rev-parse --show-toplevel)
echo ${GIT_ROOT_DIRECTORY}

echo "step 1. Configurating git hooks" 
if [ -f ${GIT_ROOT_DIRECTORY}/.git/hooks/commit-msg ]; then
	rm ${GIT_ROOT_DIRECTORY}/.git/hooks/commit-msg 
fi
if [ -f ${GIT_ROOT_DIRECTORY}/.git/hooks/prepare-commit-msg ]; then
	rm ${GIT_ROOT_DIRECTORY}/.git/hooks/prepare-commit-msg 
fi
ln ${GIT_ROOT_DIRECTORY}/infra/git-settings/commit-msg  ${GIT_ROOT_DIRECTORY}/.git/hooks/commit-msg 
ln ${GIT_ROOT_DIRECTORY}/infra/git-settings/prepare-commit-msg ${GIT_ROOT_DIRECTORY}/.git/hooks/prepare-commit-msg 
if [ $? -ne 0 ]; 
then
    echo "Error occured. Check your git hooks" 
    exit 1
else
    echo "Git hook config done."
fi

echo "step 2. Identifying diff between bigcase and smallcase" 
git config --local core.ignorecase false
if [ $? -ne 0 ]; 
then
    echo "Error occured. Check your ignorecase settings" 
    exit 1
else
    echo "Git ignorecase config done."
fi

echo "step 3. Configurating commit message template"
git config --local commit.template ./infra/git-settings/.gitmessage.txt
git config --local core.commentChar ";"
if [ $? -ne 0 ]; 
then
    echo "Error occured. Check your ignorecase settings" 
    exit 1
else
    echo "Git commit template config done."
fi

