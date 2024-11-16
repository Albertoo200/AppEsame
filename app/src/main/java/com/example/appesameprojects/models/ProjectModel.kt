package com.example.appesameprojects.models

data class ProjectModel (
    val projectId : String = "",
    val name : String ?= null,
    val projectLeaderName : String ?= null,
    val expirationDate : String ?= null,
    var projectProgress : String ?= "0%",

    var isNotified : Boolean = false,

    val projectLeaderId : String = "",
    val projectManagerId : String = "",
)