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

sudo pip install --upgrade bumpversion
bumpversion patch --message "{current_version} → {new_version}. Changes: ${changes}"

echo "Override maven settings"
sudo cp -rf maven/settings.xml /usr/share/maven/conf
echo "Import gpg key"
export GPG_TTY=$(tty)
echo -e "$GPG_KEY" | gpg --import --passphrase "$GPG_PASSPHRASE" --pinentry-mode loopback
echo "Uploading lumigo java tracer to maven central repository"
mvn -f agent/pom.xml clean package
mvn -Dmaven.test.skip=true -Dfindbugs.skip=true clean deploy
mvn nexus-staging:release

echo "Create release tag"
push_tags

echo "Done"
