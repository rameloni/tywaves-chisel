run: clean
	@echo "Running all examples once at a time"
	@for example in $(shell ls .); do \
		if [ $$(ls $$example | grep -c ".scala$$") -ne 0 ]; then \
			echo $$example; \
			scala-cli test $$example > /dev/null 2>&1; \
		fi; \
	done

clean: 
	$(RM) -rf test_run_dir/
	$(RM) *.vcd *.fir