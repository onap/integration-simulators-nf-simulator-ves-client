all: start

.PHONY: start

start:
	@echo "##### Start PNF simulator #####"
	docker-compose up -d
	@echo "##### DONE #####"

stop:
	@echo "##### Stop PNF simulator #####"
	docker-compose down
	@echo "##### DONE #####"
