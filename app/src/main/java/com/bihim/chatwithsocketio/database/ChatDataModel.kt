package com.bihim.chatwithsocketio.database

import io.realm.RealmObject

open class ChatDataModel : RealmObject() {
    var id = 0
    var viewType = 0
    var userName: String? = null
    var message: String? = null
}