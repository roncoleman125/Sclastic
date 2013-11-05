if [ $# -ne 1 ]
then
  echo usage: commit.ksh file
  exit 1
fi
git add $1
git commit
git push -u origin2 master
