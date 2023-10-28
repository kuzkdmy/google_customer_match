package com.test.gcm.domain

case class OAuthAttempt(id: String, redirectUrl: String, status: String)

case class OAuthConnection(accessToken: String, refreshToken: String)
