hosts=("dh2026pc22", "dh2026pc23", "dh2026pc24", "dh2026pc25")

remote_dir="/student/abdul322/a1group05/a1/serverSqlite"

for host in "${hosts[@]}"; do
    echo "Starting URLShortner on $host"

    ssh $host "cd $remote_dir && ./runit.bash"
    
    echo "Started URLShortner on $host"
done

echo "All hosts have been started."