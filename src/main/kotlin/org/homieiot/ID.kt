package org.homieiot

/**
 * An MQTT topic consists of one or more topic levels, separated by the slash character (/). A topic level ID MAY contain lowercase letters from a to z, numbers from 0 to 9 as well as the hyphen character (-).
 * A topic level ID MUST NOT start or end with a hyphen (-). The special character $ is used and reserved for Homie attributes.
 */
private val ID_REGEX = Regex("^[a-z0-9]+[a-z0-9\\-_]*[^-_]$")

/**
 * Requires that the supplied string meets the requirements of homie convention topic ID
 * @param id ID to check for convention compliance
 * @throws IllegalArgumentException if ID does not match convention
 */
internal fun idRequire(id: String) = require(ID_REGEX.matches(id)) { "$id may only contain lowercase letters from a to z, numbers from 0 to 9 as well as the hyphen character (-) and id cannot start or end with a hyphen (-)" }


