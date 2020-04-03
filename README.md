# git-flow-playing

Branches (case-sensitive)
- **master** - auto-deployed to production
- master -> **develop** - auto-deployed to dev0
- master -> **hotfix/x.y.z** - auto-deployed to stage0
- develop -> **release/x.y.z** - auto-deployed to stage0
- develop -> **feature/anything** - deployed to test{0-4}, dev{1-4}. stage{1-4}
- release/x.y.z -> **bugfix/anything**
- feature/anything -> **anything**


how to build or deploy feature:
in commit message --push-docker
in commit message --deploy-test0

Dev process:
- create feature/* from develop
- deploy feature to test environment
- when tested and ready to go then merge to develop
- go to develop and run `gradle startRelease` or `gradle startRelease --scope MAJOR`
- check release is up to date with master and test on stage0
- don't add/remove features in release, only bugfix
- merge release to master and develop
- for hotfix go to master and run `gradle startHotfix`
- merge hotfix to master and develop and release if present
- after any merge to master tag master with vX.Y.Z