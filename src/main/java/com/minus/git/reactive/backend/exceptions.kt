package com.minus.git.reactive.backend

class UnsupportedGitCmdException(val cmd: GitConst.Cmd) : Throwable()
