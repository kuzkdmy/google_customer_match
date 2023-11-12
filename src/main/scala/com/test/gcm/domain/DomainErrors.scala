package com.test.gcm.domain

import sttp.model.StatusCode

sealed trait DomainErrors { _: Throwable => }

case class NotFoundError(id: String, entityName: String) extends Exception(s"$entityName $id not found") with DomainErrors
case class CustomError(message: String, code: StatusCode) extends Exception(message) with DomainErrors
