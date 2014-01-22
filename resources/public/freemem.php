<?php

$fh = fopen('/proc/meminfo', 'r');
$data = array();

while ($line = fgets($fh)) {
    preg_match('/^(.*):\s+(\d+)\s/', $line, $m);
    if ($m) {
        $data[$m[1]] = $m[2];
    }
}

$free = $data['MemFree'] + $data['Cached'];
$timestamp = time();

header('Content-type: application/json');
echo json_encode(array('free mem' => array($timestamp => $free)));

?>
