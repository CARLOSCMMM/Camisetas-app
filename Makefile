PANDO = pandoc
# FLAGS = --top-level-division=chapter --listings -r markdown-auto_identifiers -w latex -o
FLAGS = --top-level-division=chapter --listings -o

all:
	$(PANDO) 0*.md 1*.md 9*.md -o Libro.pdf --template docs/eisvogel --listings --number-sections --pdf-engine=xelatex

clean:
	rm *aux