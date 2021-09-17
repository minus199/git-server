package com.minus.git.reactive.service


class RepositoryExistsException(val repoName: String) : Exception("repo $repoName already exists")