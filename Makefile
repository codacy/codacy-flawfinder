all: publish-base generate-docs

publish-base:
	docker build --no-cache -t "codacy-flawfinder-base:latest" -f Dockerfile . --build-arg toolVersion="$(shell cat .flawfinder-version | tr -d '\n')"

generate-docs:
	docker run -i codacy-flawfinder-base:latest -QD --listrules > .tmp_errorlist
	sbt "runMain codacy.flawfinder.DocGenerator .tmp_errorlist"
	rm -rf .tmp_errorlist
