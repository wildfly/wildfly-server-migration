#
# JBoss Server Migration Tool for Windows
#
# A simple tool for migrating servers.
#

Function String-To-Array($value) {
    $res = @()
    if (!$value){
        return $res
    }
    $tmpArr = $value.split()

    foreach ($str in $tmpArr) {
        if ($str) {
            $res += $str
        }
    }
    return $res
}

$TOOL_OPTS = @()
for($i=0; $i -lt $args.Count; $i++){
    if ($Args[$i].Substring(0, 2) -ne "-D") {
        $TOOL_OPTS += $Args[$i]
    }
}

$BASE_DIR = $PSScriptRoot

$JAVA_OPTS = @()
if(Test-Path env:JAVA_OPTS) {
    $javaOpts = (Get-ChildItem env:JAVA_OPTS).Value
    $JAVA_OPTS = String-To-Array -value $javaOpts
}
for($i=0; $i -lt $args.Count; $i++){
    if ($Args[$i].Substring(0, 2) -eq "-D") {
        $JAVA_OPTS += $Args[$i]
    }
}

if (!(Test-Path env:JAVA)) {
    if(Test-Path env:JAVA_HOME) {
        $JAVA_HOME = (Get-ChildItem env:JAVA_HOME).Value
        $JAVA = $JAVA_HOME + "\bin\java.exe"
    } else {
        $JAVA = 'java'
    }
}

$DEFAULT_MODULAR_JVM_OPTIONS = @()

& $JAVA --add-modules java.se -version >$null 2>&1
if ($LastExitCode -eq 0){
    if (-Not ($JAVA_OPTS -match ('--add-modules'))){
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED"
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=jdk.unsupported/sun.reflect=ALL-UNNAMED"
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-modules=java.se"
    }
}

$PROG_ARGS = @()
if ($JAVA_OPTS -ne $null){
    $PROG_ARGS += $JAVA_OPTS
}
if ($DEFAULT_MODULAR_JVM_OPTIONS -ne $null){
    $PROG_ARGS += $DEFAULT_MODULAR_JVM_OPTIONS
}

$PROG_ARGS += "-Djboss.server.migration.baseDir=$BASE_DIR"

if (-Not ($JAVA_OPTS -match ('logging.configuration'))){
    $PROG_ARGS += "-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
    $PROG_ARGS += "-Dlogging.configuration=file:$BASE_DIR\configuration\logging.properties"
    $PROG_ARGS += "-Djboss.server.migration.logfile=$BASE_DIR\logs\migration.log"
}

& $JAVA $PROG_ARGS -cp "$BASE_DIR\lib\*" org.jboss.migration.cli.CommandLineServerMigration $TOOL_OPTS