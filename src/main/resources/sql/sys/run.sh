POSTGRES_USER="hub"
POSTGRES_PASSWORD="st@skyhub2016"
DATABASE_NAME="hubdb"

if [[ $# -eq 0 ]] ; then
    echo 'Please enter the folder where you want to import all *.sql files under it.'
    exit 0
fi

echo "Cleaning up cache..."
/etc/init.d/postgresql stop
echo 1 > /proc/sys/vm/drop_caches
/etc/init.d/postgresql start

echo "Importing..."

export PGPASSWORD=$POSTGRES_PASSWORD

for file in `find $1 | grep -i '.sql'` 
do 
  echo "importing $file"
  psql -U $POSTGRES_USER $DATABASE_NAME < $file
done