package com.minus.git.reactive.http

import org.springframework.http.HttpStatus

class ApiException(msg:String, val status: HttpStatus):Exception(msg)
