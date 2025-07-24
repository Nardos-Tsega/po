# File: scripts/run-tests.sh

echo "🧪 Running Payment Gateway Test Suite"
echo "======================================"

# Unit tests
echo "📝 Running Unit Tests..."
mvn test -Dtest="*Test" -DfailIfNoTests=false

# Integration tests  
echo "🔗 Running Integration Tests..."
mvn test -Dtest="*IntegrationTest" -DfailIfNoTests=false

# Load tests
echo "⚡ Running Load Tests..."
mvn test -Dtest="*LoadTest" -DfailIfNoTests=false

# Generate test report
echo "📊 Generating Test Report..."
mvn surefire-report:report

echo "✅ All tests completed!"
echo "📄 Test report: target/site/surefire-report.html"