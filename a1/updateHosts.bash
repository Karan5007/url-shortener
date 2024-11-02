#!/bin/bash

for i in {1..23}
do
  ssh-keygen -R "dh2010pc$i.utm.utoronto.ca"
done