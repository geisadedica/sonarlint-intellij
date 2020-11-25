package org.sonarlint.intellij.its

import com.sonar.orchestrator.Orchestrator
import com.sonar.orchestrator.container.Server
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import org.sonarqube.ws.client.HttpConnector
import org.sonarqube.ws.client.WsClient
import org.sonarqube.ws.client.WsClientFactories

const val SONARLINT_USER = "sonarlint"
const val SONARLINT_PWD = "sonarlintpwd"

abstract class AbstractConnectedTest {

    companion object {

        @ClassRule
        @JvmField
        var t = TemporaryFolder()

        @JvmStatic
        protected fun newAdminWsClient(orchestrator: Orchestrator): WsClient {
            val server = orchestrator.server
            return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
                    .url(server.url)
                    .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
                    .build())
        }

    }
}
