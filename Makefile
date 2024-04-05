.PHONY: all download-surfer clean

TYWAVES_SURFER_REPO=https://gitlab.com/rameloni/surfer-tywaves-demo.git
TYWAVES_BRANCH=tywaves
TYWAVES_NAME=surfer-tywaves-demo

CHISEL_FORK_REPO=https://github.com/rameloni/chisel.git

all: install-surfer-tywaves install-chisel-fork clean install-tywaves-backend

create-tmp:
	@mkdir -p tmp/

install-surfer-tywaves: create-tmp
	@cd tmp/ && git clone -b $(TYWAVES_BRANCH) $(TYWAVES_SURFER_REPO) && cd $(TYWAVES_NAME) && git submodule update --init --recursive
	@cd tmp/$(TYWAVES_NAME) && cargo install --path .

clean:
	@rm -rf tmp/

install-chisel-fork: create-tmp
	@cd tmp/ && git clone $(CHISEL_FORK_REPO)
	@cd tmp/chisel && sbt "unipublish / publishLocal"

install-tywaves-backend:
	@sbt publishLocal
