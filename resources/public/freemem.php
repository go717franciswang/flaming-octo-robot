<?php

$fh = fopen('/proc/meminfo', 'r');
$data = array();

while ($line = fgets($fh)) {
    preg_match('/^(.*):\s+(\d+)\s/', $line, $m);
    if ($m) {
        $data[$m[1]] = $m[2];
    }
}

$unallocated = (int) $data['MemFree'];
$cached = (int) $data['Cached'];
$free = $unallocated + $cached;
$timestamp = time() . "000";

header('Content-type: application/json');
echo json_encode(array($timestamp => array(
    'free mem' => $free,
    'unallocated' => $unallocated,
    'cached' => $cached,
)));

?>
