.PHONY: all download-surfer clean

TYWAVES_SURFER_REPO=https://gitlab.com/rameloni/surfer-tywaves-demo.git
TYWAVES_BRANCH=v0.2.1-tywaves-dev-SNAPSHOT
TYWAVES_NAME=surfer-tywaves-demo

CHISEL_FORK_REPO=https://github.com/rameloni/chisel.git
CHISEL_FORK_BRANCH=v6.1.0-tywaves-SNAPSHOT

all: install-surfer-tywaves install-chisel-fork clean install-tywaves-backend

create-tmp:
	@mkdir -p tmp/

install-surfer-tywaves: create-tmp
	@cd tmp/ && git clone -b $(TYWAVES_BRANCH) $(TYWAVES_SURFER_REPO) && cd $(TYWAVES_NAME) && git submodule update --init --recursive
	@cd tmp/$(TYWAVES_NAME) && cargo install --path .

clean:
	@rm -rf tmp/

install-chisel-fork: create-tmp
	@cd tmp/ && git clone $(CHISEL_FORK_REPO) && cd chisel && git checkout $(CHISEL_FORK_BRANCH)
	@cd tmp/chisel && sbt "unipublish / publishLocal"

install-tywaves-backend:
	@sbt publishLocal
