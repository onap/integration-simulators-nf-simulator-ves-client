all: start

.PHONY: start

start:
	@echo "##### Start Ves client #####"
	docker-compose up -d
	@echo "##### DONE #####"

stop:
	@echo "##### Stop Ves client #####"
	docker-compose down
	@echo "##### DONE #####"
