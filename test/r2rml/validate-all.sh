./validate test/r2rml/valid/well-formed1.ttl
for file in test/r2rml/valid/*.ttl
do
    ./validate -l test/r2rml/validator-test-schema.sql $file
done
for file in test/r2rml/warning/*.ttl
do
    ./validate -l test/r2rml/validator-test-schema.sql $file
done
for file in test/r2rml/invalid/*.ttl
do
    ./validate -l test/r2rml/validator-test-schema.sql $file
done
