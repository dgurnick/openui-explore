package com.dgurnick.openuiexplore.data.network

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
