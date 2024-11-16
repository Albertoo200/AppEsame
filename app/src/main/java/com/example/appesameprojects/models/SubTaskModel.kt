package com.example.appesameprojects.models

data class SubTaskModel(
    val subTaskId : String = "",
    var name : String ?= null,
    var priority : String ?= null,
    var status : String ?= null,
    var expirationDate : String ?= null,
    var subTaskProgress : String ?= null,

    var isNotified : Boolean = false,

    val taskId : String = "",
    val taskName : String ?= null,
    val projectId : String = "",
    val projectName : String ?= null,
    val projectLeaderId : String = "",
    val developerId : String = ""

)