.PHONY: all download-surfer clean

TYWAVES_SURFER_REPO=https://gitlab.com/rameloni/surfer-tywaves-demo.git
TYWAVES_BRANCH=tywaves
TYWAVES_NAME=surfer-tywaves-demo

all: install-surfer-tywaves clean install-tywaves-backend

install-surfer-tywaves:
	@mkdir -p tmp/
	@cd tmp/ && git clone -b $(TYWAVES_BRANCH) $(TYWAVES_SURFER_REPO) && cd $(TYWAVES_NAME) && git submodule update --init --recursive
	@cd tmp/$(TYWAVES_NAME) && cargo install --path .

clean:
	@rm -rf tmp/

install-tywaves-backend:
	@sbt publishLocal
