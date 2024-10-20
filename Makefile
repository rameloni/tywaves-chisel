.PHONY: all download-surfer clean
# Tywaves Surfer information
TYWAVES_SURFER_NAME=surfer-tywaves-demo
TYWAVES_SURFER_REPO=https://gitlab.com/rameloni/${TYWAVES_SURFER_NAME}.git
TYWAVES_SURFER_VERSION=0.3.2
TYWAVES_SURFER_TAG=v${TYWAVES_SURFER_VERSION}-tywaves-SNAPSHOT
TYWAVES_SURFER_BIN=surfer-tywaves
TYWAVES_SURFER_TARGET_NAME=${TYWAVES_SURFER_BIN}-${TYWAVES_SURFER_VERSION}
TYWAVES_SURFER_INSTALL_PATH=$(HOME)/.cargo/bin/

# Chisel information
CHISEL_FORK_REPO=https://github.com/rameloni/chisel.git
CHISEL_FORK_TAG=v6.4.3-tywaves-SNAPSHOT

# Circt (firtool) information
CIRCT_FIRTOOL_ZIP_NAME=firtool-bin-linux-x64.tar.gz
CIRCT_FORK_VERSION=0.1.5
CIRCT_FORK_TAG=v${CIRCT_FORK_VERSION}-tywaves-SNAPSHOT
CIRCT_FORK_FIRTOOL_ZIP_LINK=https://github.com/rameloni/circt/releases/download/${CIRCT_FORK_TAG}/${CIRCT_FIRTOOL_ZIP_NAME}
CIRCT_FIRTOOL_NAME=firtool-type-dbg-info-${CIRCT_FORK_VERSION}
CIRCT_FIRTOOL_INSTALL_PATH=$(HOME)/.local/bin/

all: install-surfer-tywaves install-chisel-fork install-firtool-fork-bin clean install-tywaves-chisel-api

create-tmp:
	@mkdir -p tmp/

install-surfer-tywaves-dev: create-tmp
	@echo "Installing Surfer TyWaves from main branch"
	@cd tmp/ && git clone -b main $(TYWAVES_SURFER_REPO) && cd $(TYWAVES_SURFER_NAME) && git submodule update --init --recursive
	@#cd tmp/$(TYWAVES_SURFER_NAME) && cargo install --path .
	@cd tmp/$(TYWAVES_SURFER_NAME) && cargo build --release
	@cp tmp/$(TYWAVES_SURFER_NAME)/target/release/$(TYWAVES_SURFER_BIN) $(TYWAVES_SURFER_INSTALL_PATH)$(TYWAVES_SURFER_TARGET_NAME)
	echo "installed $(TYWAVES_SURFER_TARGET_NAME) in $(TYWAVES_SURFER_INSTALL_PATH)"

install-surfer-tywaves: create-tmp
	@cd tmp/ && git clone -b $(TYWAVES_SURFER_TAG) $(TYWAVES_SURFER_REPO) && cd $(TYWAVES_SURFER_NAME) && git submodule update --init --recursive
	@#cd tmp/$(TYWAVES_SURFER_NAME) && cargo install --path .
	@cd tmp/$(TYWAVES_SURFER_NAME) && cargo build --release
	@cp tmp/$(TYWAVES_SURFER_NAME)/target/release/$(TYWAVES_SURFER_BIN) $(TYWAVES_SURFER_INSTALL_PATH)$(TYWAVES_SURFER_TARGET_NAME)
	echo "installed $(TYWAVES_SURFER_TARGET_NAME) in $(TYWAVES_SURFER_INSTALL_PATH)"

clean:
	$(RM) -rf tmp/

install-chisel-fork: create-tmp
	@cd tmp/ && git clone $(CHISEL_FORK_REPO) && cd chisel && git checkout $(CHISEL_FORK_TAG)
	@cd tmp/chisel && sbt "unipublish / publishLocal"

install-tywaves-chisel-api: install-chisel-fork
	@sbt reload && sbt compile && sbt publishLocal

clean-firtool-fork-bin:
	$(RM) tmp/$(CIRCT_FIRTOOL_ZIP_NAME)*

install-firtool-fork-bin: clean-firtool-fork-bin create-tmp
	# Extract the firtool binary from the forked circt repository
	@cd tmp/ && wget $(CIRCT_FORK_FIRTOOL_ZIP_LINK) && tar -xf $(CIRCT_FIRTOOL_ZIP_NAME)
	@cp tmp/bin/$(CIRCT_FIRTOOL_NAME) $(CIRCT_FIRTOOL_INSTALL_PATH)