
## Contributing to jobrunr
JobRunr follows a very standard Github development process, using Github tracker for issues and merging pull requests into master. If you want to contribute even something trivial please do not hesitate, but follow the guidelines below.

### Contact via GitHub Discussions

GitHub Discussions is the recommended way for discussing almost anything that relates to JobRunr. We look forward to your input!

### Reporting issue

Please follow the [template](https://github.com/jobrunr/jobrunr/issues/new?template=bug_report.md&title=%5BBUG%5D) for reporting any issues.

### Code Conventions
Our code style is almost in line with the standard java conventions (Popular IDE's default setting satisfy this), with the following additional restricts:  

* Unit tests should be added for a new feature or an important bugfix.
* If no-one else is using your branch, please rebase it against the current master (or other target branch in the main project).
* When writing a commit message please follow these conventions, if you are fixing an existing issue please add Fixes #XXX at the end of the commit message (where XXX is the issue number).

### Contribution flow
Before contribution to JobRunr, you must agree to the [JobRunr Contributors Agreement](https://github.com/jobrunr/jobrunr/blob/master/CLA.md). Once you have reviewed and agree with it, please sign the Contributors Agreement by adding yourself to the contributors of JobRunr as [explained here](https://github.com/jobrunr/clabot-config). 

This is a rough outline of what a contributor's workflow looks like:

* Fork the current repository
* Create a topic branch from where to base the contribution. This is usually master.
* Make commits of logical units.
* Make sure commit messages are in the proper format.
* Push changes in a topic branch to your forked repository.
* Before you send out the pull request, please sync your forked repository with remote repository, this will make your pull request simple and clear. See guide below:
```
git remote add upstream git@github.com:jobrunr/jobrunr.git
git fetch upstream
git rebase upstream/master
git checkout -b your_awesome_patch
... add some work
git push origin your_awesome_patch
```
* Submit a pull request to jobrunr/jobrunr and wait for the reply.

Thanks for contributing!
