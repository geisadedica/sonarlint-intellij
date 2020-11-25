package org.sonarlint.intellij.its

import com.google.protobuf.InvalidProtocolBufferException
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.StepLogger
import com.intellij.remoterobot.stepsProcessing.StepWorker
import com.sonar.orchestrator.Orchestrator
import com.sonar.orchestrator.build.MavenBuild
import com.sonar.orchestrator.locator.FileLocation
import com.sonar.orchestrator.locator.MavenLocation
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.sonarlint.intellij.its.ItUtils.SONAR_VERSION
import org.sonarqube.ws.client.WsClient
import org.sonarqube.ws.client.hotspots.SearchRequest
import org.sonarqube.ws.client.users.CreateRequest
import org.sonarqube.ws.client.usertokens.GenerateRequest
import java.awt.event.KeyEvent
import java.net.URL
import java.nio.file.Path

const val PROJECT_KEY_JAVA = "sample-java"
const val BIND_PROJECT_CHECKBOX = "//div[@accessiblename='Bind project to SonarQube / SonarCloud' and @class='JBCheckBox' and @text='Bind project to SonarQube / SonarCloud']"

class OpenInIdeTest : AbstractConnectedTest() {

    @Rule
    @JvmField
    var exception = ExpectedException.none()

    companion object {

        fun getOpenHotspotRequest(): String {
            return "http://localhost:64120/sonarlint/api/hotspots/show?project=$PROJECT_KEY_JAVA&hotspot=$firstHotspotKey&server=${ORCHESTRATOR.server.url}"
        }

        @ClassRule
        @JvmField
        val temp = TemporaryFolder()

        private lateinit var adminWsClient: WsClient
        private var sonarUserHome: Path? = null

        private var firstHotspotKey: String? = null
        var token: String? = null

        @ClassRule
        @JvmField
        val ORCHESTRATOR = Orchestrator.builderEnv()
                .setSonarVersion(SONAR_VERSION)
                .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", ItUtils.javaVersion))
                .restoreProfileAtStartup(FileLocation.ofClasspath("/java-sonarlint-with-hotspot.xml"))
                .build()

        @BeforeClass
        @Throws(java.lang.Exception::class)
        fun prepare() {

            adminWsClient = newAdminWsClient(ORCHESTRATOR)
            sonarUserHome = temp.newFolder().toPath()
            adminWsClient.users()?.create(CreateRequest().setLogin(SONARLINT_USER).setPassword(SONARLINT_PWD).setName("SonarLint"))

            ORCHESTRATOR.server.provisionProject(PROJECT_KEY_JAVA, "Sample Java")
            ORCHESTRATOR.server.associateProjectToQualityProfile(PROJECT_KEY_JAVA, "java", "SonarLint IT Java")


            // Build project to have bytecode
            ORCHESTRATOR.executeBuild(MavenBuild.create(java.io.File("projects/sample-java/pom.xml")).setGoals("clean compile"))
            //prepareRedirectServer()
            firstHotspotKey = getFirstHotspotKey(adminWsClient, PROJECT_KEY_JAVA)
            val generateRequest = GenerateRequest()
            generateRequest.name = "TestUser"
            token = adminWsClient.userTokens().generate(generateRequest).token

        }

//        @Throws(Exception::class)
//        private fun prepareRedirectServer() {
//            redirectPort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress())
//            val threadPool = QueuedThreadPool()
//            threadPool.maxThreads = 500
//            server = Server(threadPool)
//
//            // HTTP Configuration
//            val httpConfig = HttpConfiguration()
//            httpConfig.sendServerVersion = true
//            httpConfig.sendDateHeader = false
//
//            // Moved handler
//            val movedContextHandler = MovedContextHandler()
//            movedContextHandler.isPermanent = true
//            movedContextHandler.newContextURL = ORCHESTRATOR.server.url
//            server.handler = movedContextHandler
//
//            // http connector
//            val http = ServerConnector(server, HttpConnectionFactory(httpConfig))
//            http.port = redirectPort
//            server.addConnector(http)
//            server.start()
//        }

    }

//    @Test
//    fun test() = uiTest {
//        removeBinding(this)
//    }

    @Test
    fun fullPath() = uiTest {
        cleanConnectionAndBinding(this)
        URL(getOpenHotspotRequest()).readText()
        agreeToCreateConnection(this)
        agreeToBindRecentProject(this)
        checkFileOpened(this)
    }

//    @Test
//    fun withCreatedConnection() = uiTest {
//        cleanConnectionAndBinding(this)
//        createConnection(this)
//        URL(OPEN_HOTSPOT_REQUEST).readText()
//        agreeToBindRecentProject(this)
//        checkFileOpened(this)
//    }
//
//    // TODO remove extra press enter
//    @Test
//    fun withBoundProject() = uiTest {
//        cleanConnectionAndBinding(this)
//        bindProject(this)
//        URL(OPEN_HOTSPOT_REQUEST).readText()
//        agreeToCreateConnection(this)
//        checkFileOpened(this)
//    }

    private fun bindProject(robot: RemoteRobot) {
        with(robot) {
            openSonarLintProjectSettings(this)
            find<ComponentFixture>(byXpath(BIND_PROJECT_CHECKBOX)).click()

        }
    }


    private fun removeConnection(robot: RemoteRobot) {
        with(robot) {
            openSonarLintSettings(this)
            if (findAll<ContainerFixture>(byXpath("//div[@text = '(https://next.sonarqube.com/sonarqube)']")).isEmpty()) {
                find<ComponentFixture>(byXpath("//div[@myaction = 'Remove (Remove)']")).click()
                Thread.sleep(100)
                runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
                find<ComponentFixture>(byXpath("//div[@accessiblename='OK' and @class='JButton' and @text='OK']")).click()
            }
        }
    }

    private fun removeBinding(robot: RemoteRobot) {
        with(robot) {
            openSonarLintProjectSettings(this)
            find<ComponentFixture>(byXpath(BIND_PROJECT_CHECKBOX)).click()
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
        }
    }

    private fun cleanConnectionAndBinding(robot: RemoteRobot) {
        with(robot) {
            removeConnection(this)
            removeBinding(this)
        }
    }

    private fun agreeToBindRecentProject(robot: RemoteRobot) {
        with(robot) {
            find<ComponentFixture>(byXpath("//div[@accessiblename='Opening Security Hotspot...' and @class='MyDialog']")).apply {
                find<ComponentFixture>(byXpath("//div[@accessiblename='Select project' and @class='JButton' and @text='Select project']")).click()
            }
            Thread.sleep(1000)
            find<ComponentFixture>(byXpath("//div[@accessiblename='Select a project' and @class='MyDialog']")).apply {
                findText("sonarlint-core-parent").click()
            }
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
            Thread.sleep(5000)
        }
    }

    private fun openSettings(robot: RemoteRobot) {
        with(robot) {
            find<ComponentFixture>(byXpath("//div[@text = 'File']")).click()
            find<ContainerFixture>(byXpath("//div[@class='HeavyWeightWindow']"))
                    .find<ComponentFixture>(byXpath("//div[@text = 'Settings...']")).click()
        }
    }

    private fun openSonarLintSettings(robot: RemoteRobot) {
        with(robot) {
            openSettings(this)
            Thread.sleep(500)
            runJs("robot.enterText('SonarLint')")
            // TODO add more specific check
            while (findAll<ContainerFixture>(byXpath("//div[@myaction = 'Add (Add)']")).isEmpty()) {
                runJs("robot.pressAndReleaseKey(${KeyEvent.VK_DOWN})")
                Thread.sleep(100)
            }

        }
    }

    // TODO doesn't stop for some reason
    private fun openSonarLintProjectSettings(robot: RemoteRobot) {
        with(robot) {
            openSettings(this)
            Thread.sleep(500)
            runJs("robot.enterText('SonarLint')")
            while (findAll<ContainerFixture>(byXpath(BIND_PROJECT_CHECKBOX)).isEmpty()) {
                runJs("robot.pressAndReleaseKey(${KeyEvent.VK_DOWN})")
                Thread.sleep(100)
            }
        }
    }

    private fun createConnection(robot: RemoteRobot) {
        with(robot) {
            openSonarLintSettings(this)

            find<ComponentFixture>(byXpath("//div[@myaction = 'Add (Add)']")).click()

            runJs("robot.enterText('Next')")

            findAll<ContainerFixture>(byXpath("//div[@class='JRadioButton']"))[1].click()
            findAll<ContainerFixture>(byXpath("//div[@class='JBTextField']"))[2].click()
            runJs("robot.enterText('http://next.sonarqube.com/sonarqube')")

            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")

            findAll<ContainerFixture>(byXpath("//div[@class='JBTextField']"))[0].click()
            runJs("robot.enterText('Next')")
            Thread.sleep(500)
            find<ContainerFixture>(byXpath("//div[@accessiblename='New Connection: Authentication' and @class='MyDialog']"))
                    .find<ContainerFixture>(byXpath("//div[@class='JTextField']")).click()
            runJs("robot.enterText('$token')")
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
            Thread.sleep(5000)
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
            Thread.sleep(5000)
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
            Thread.sleep(5000)
            find<ComponentFixture>(byXpath("//div[@accessiblename='OK' and @class='JButton' and @text='OK']")).click()
            Thread.sleep(500)
        }
    }

    private fun openFromStorage(robot: RemoteRobot) {
        with(robot) {
            find<ComponentFixture>(byXpath("//div[@accessiblename='Open or import' and @class='JButton' and @text='Open or import']")).click()
            //TODO escape backslashes
            runJs("robot.enterText('C:\\Users\\knize\\IdeaProjects\\Sonar\\sonarlint-core')")
        }
    }

    private fun checkFileOpened(robot: RemoteRobot) {
        with(robot) {
            val file = "VersionUtils.java"
            find<ContainerFixture>(byXpath("//div[@accessiblename='Editor for $file' and @class='EditorComponentImpl']")).click()
        }
    }

    private fun agreeToCreateConnection(robot: RemoteRobot) {
        with(robot) {
            find<ComponentFixture>(byXpath("//div[@accessiblename='Create connection' and @class='JButton' and @text='Create connection']")).click()

            runJs("robot.enterText('Next')")

            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")

            find<ContainerFixture>(byXpath("//div[@accessiblename='New Connection: Authentication' and @class='MyDialog']"))
                    .find<ContainerFixture>(byXpath("//div[@class='JTextField']")).click()
            runJs("robot.enterText('$token')")
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
            Thread.sleep(1000)
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
            Thread.sleep(1000)
            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
            Thread.sleep(10000)

        }
    }
//
//    fun agreeToCreateConnection(robot: RemoteRobot) {
//        with(robot){
//            find<ComponentFixture>(byXpath("//div[@text = 'File']")).click()
//            find<ContainerFixture>(byXpath("//div[@class='HeavyWeightWindow']"))
//                    .find<ComponentFixture>(byXpath("//div[@text = 'Settings...']")).click()
//
//            runJs("robot.enterText('SonarLint')")
//            while (findAll<ContainerFixture>(byXpath("//div[@myaction = 'Add (Add)']")).isEmpty()) {
//                runJs("robot.pressAndReleaseKey(${KeyEvent.VK_DOWN})")
//                Thread.sleep(100)
//            }
//
//            find<ComponentFixture>(byXpath("//div[@myaction = 'Add (Add)']")).click()
//
//            runJs("robot.enterText('Next')")
//
//            findAll<ContainerFixture>(byXpath("//div[@class='JRadioButton']"))[1].click()
//            findAll<ContainerFixture>(byXpath("//div[@class='JBTextField']"))[2].click()
//            runJs("robot.enterText('http://next.sonarqube.com/sonarqube')")
//
//            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
//
//            findAll<ContainerFixture>(byXpath("//div[@class='JBTextField']"))[0].click()
//            runJs("robot.enterText('Next')")
//
//            find<ContainerFixture>(byXpath("//div[@accessiblename='New Connection: Authentication' and @class='MyDialog' and @name='dialog2']"))
//                    .find<ContainerFixture>(byXpath("//div[@class='JTextField']")).click()
//            runJs("robot.enterText('97aeba2f39766bced6241680544c098bba3fc3cc')")
//            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
//            Thread.sleep(500)
//            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
//            Thread.sleep(500)
//            runJs("robot.pressAndReleaseKey(${KeyEvent.VK_ENTER})")
//            Thread.sleep(500)
//        }
//    }

}

@Throws(InvalidProtocolBufferException::class)
private fun getFirstHotspotKey(client: WsClient, projectKey: String): String? {
    val searchRequest = SearchRequest()
    searchRequest.projectKey = projectKey
    val searchResults = client.hotspots().search(searchRequest)
    val hotspot = searchResults.hotspotsList[0]
    return hotspot.key
}


fun uiTest(url: String = "http://localhost:8082", test: RemoteRobot.() -> Unit) {
    RemoteRobot(url).apply(test)
}

object StepsLogger {
    private var initializaed = false

    @JvmStatic
    fun init() = synchronized(initializaed) {
        if (initializaed.not()) {
            StepWorker.registerProcessor(StepLogger())
            initializaed = true
        }
    }
}

