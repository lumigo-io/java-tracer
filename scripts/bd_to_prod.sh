#!/usr/bin/env bash
set -Eeo pipefail

setup_git() {
    git config --global user.email "no-reply@build.com"
    git config --global user.name "CircleCI"
    git checkout master
    # Avoid npm version failure
    git stash
}

push_tags() {
    git push origin master --tags
}

echo ".____                  .__                  .__        ";
echo "|    |    __ __  _____ |__| ____   ____     |__| ____  ";
echo "|    |   |  |  \/     \|  |/ ___\ /  _ \    |  |/  _ \ ";
echo "|    |___|  |  /  Y Y  \  / /_/  >  <_> )   |  (  <_> )";
echo "|_______ \____/|__|_|  /__\___  / \____/ /\ |__|\____/ ";
echo "        \/           \/  /_____/         \/            ";
echo
echo "Deploy lumigo-java-tracer to maven repository server"

setup_git
echo "Getting latest changes from git"
changes=$(git log $(git describe --tags --abbrev=0)..HEAD --oneline)

pip install --upgrade bumpversion
bumpversion patch --message "{current_version} → {new_version}. Changes: ${changes}"

echo "Override maven settings"
cp -rf maven/settings.xml /usr/share/maven/conf
echo "Import gpg key"
export GPG_TTY=$(tty)
gpg --batch --passphrase "$GPG_PASSPHRASE" --import java-sign-keys/private.asc
echo "Uploading lumigo java tracer to maven central repository"
mvn -f agent/pom.xml clean deploy
mvn -f agent/pom.xml nexus-staging:release
mvn -Dmaven.test.skip=true -Dfindbugs.skip=true clean deploy
mvn nexus-staging:release

echo "Creating lumigo-java-tracer layer"
./scripts/prepare_layer_files.sh

echo "Creating layer latest version arn table md file (LAYERS.md)"
commit_version="$(git describe --abbrev=0 --tags)"
../utils/common_bash/create_layer.sh \
    --layer-name lumigo-java-tracer \
    --region ALL \
    --package-folder python \
    --version "$commit_version" \
    --runtimes "java11 java17 java21"

cd ../larn && npm i -g
larn -r java11 -n layers/LAYERS.md --filter lumigo-java-tracer -p ~/java-tracer
cd ../java-tracer

git add layers/LAYERS.md
git commit -m "docs: update layers md [skip ci]"
git push origin master

echo "Create release tag"
push_tags

echo "Done"
