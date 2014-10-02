buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "com.jfrog.bintray.gradle:gradle-bintray-plugin:0.6"
    }
    
}

import groovyx.net.http.HTTPBuilder
import com.jfrog.bintray.gradle.BintrayHttpClientFactory
import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*

def testRepo
def testPackageName
def testDesc
def packagePath
def testVersion
def testNonExistingVersion
def apiUrl
def HTTPBuilder http

def init = {
    Properties props = new Properties()
    props.load(new FileInputStream(rootDir.toString() + "/gradle.properties"))
    
    testRepo = props.get('testRepo')
    testPackageName = props.get('testPackageName')
    testDesc = props.get('testDesc')
    testVersion = props.get('testVersion')
    testNonExistingVersion = props.get('testNonExistingVersion')
    
    apiUrl = props.get('apiUrl')
    
    packagePath = "$bintrayUser/$testRepo/$testPackageName"
    http = BintrayHttpClientFactory.create(apiUrl, bintrayUser, bintrayKey)
}

def isPackageExists = {
    def exists
    http.request(HEAD) {
        uri.path = "/packages/$packagePath"
        response.success = { resp ->
            exists = true
        }
        response.'404' = { resp ->
            exists = false
        }
    }
    return exists
}

def isVersionExists = { versionName ->
    def exists

    http.request(HEAD) {
        uri.path = "/packages/$packagePath/versions/$versionName"
        response.success = { resp ->
            exists = true;
        }
        response.'404' = { resp ->
            exists = false;
        }
    }
    return exists
}

def deletePackage = {
    if (isPackageExists()) {
        http.request(DELETE, JSON) {
            uri.path = "/packages/$packagePath"

            response.success = { resp ->
                logger.debug("Deleted package '$packagePath'.")
            }
            response.failure = { resp ->
                throw new GradleException("Could not delete package '$packagePath': $resp.statusLine")
            }
        }
    }
}

task cleanPackage << {
    init()
    deletePackage()
}

task verify << {
    if (!isPackageExists()) {
        throw new GradleException("Package '$packagePath' does not exist, although it should have been created")
    }
    if (!isVersionExists(testVersion)) {
        throw new GradleException("Version '$testVersion' does not exist, although it should have been created")
    }
    if (isVersionExists(testNonExistingVersion)) {
        throw new GradleException("Version '$testNonExistingVersion' exists, although it was not supposed to be created")
    }
}

task finalize << {
    deletePackage()
}
