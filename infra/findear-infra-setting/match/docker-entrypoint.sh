#!/bin/bash

saveDict() {
  echo "colorDict save"
  curl localhost:8000/save/color
  sleep 5
}

trap 'saveDict' TERM

poetry run python manage.py runserver 0.0.0.0:8000
sleep 999d
