package com.test.gcm.domain

case class OperateListMembersCmd(
    addMembers: List[AddMember],
    removeMembers: List[RemoveMember]
)

case class AddMember(hashedEmail: HashedEmail)
case class RemoveMember(hashedEmail: HashedEmail)
