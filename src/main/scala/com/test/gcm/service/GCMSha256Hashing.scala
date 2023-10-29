package com.test.gcm.service

import java.security.MessageDigest

object GCMSha256Hashing {

  /** https://support.google.com/google-ads/answer/7474263
    */
  def hash(value: String, trimIntermediateSpaces: Boolean): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val lower  = value.toLowerCase
    val normalized = if (trimIntermediateSpaces) {
      lower.replaceAll("\\s+", "")
    } else lower.trim
    val hash = digest.digest(normalized.getBytes("UTF-8"))
    val sb   = new StringBuilder
    hash.foreach(c => sb.append(String.format("%02x", c)))
    sb.toString
  }
}
