package com.example.appesameprojects.models

data class TaskModel(
    val taskId : String = "",
    var name : String ?= null,
    var description : String ?= null,
    val developerName : String ?= null,
    var expirationDate : String ?= null,
    var taskProgress : String ?= null,

    var isNotified : Boolean = false,

    val projectName : String ?= null,
    val projectId : String = "",
    val projectLeaderId : String = "",
    val developerId : String = ""
)