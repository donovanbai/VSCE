import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class dynamodbTest {
	static DynamoDB dynamoDB;

    public static void main(String[] args) throws Exception {
		init();
		try {
			String tableName = "users";

			ArrayList<AttributeDefinition> attrDefs = new ArrayList<AttributeDefinition>();
			attrDefs.add(new AttributeDefinition().withAttributeName("username").withAttributeType("S"));
			//attrDefs.add(new AttributeDefinition().withAttributeName("password").withAttributeType("S"));

			ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement().withAttributeName("username").withKeyType(KeyType.HASH));

			CreateTableRequest createTableRequest = new CreateTableRequest()
				.withTableName(tableName)
				.withKeySchema(keySchema)
				.withAttributeDefinitions(attrDefs)
				.withProvisionedThroughput(new ProvisionedThroughput()
					.withReadCapacityUnits(1L)
					.withWriteCapacityUnits(1L));

			//Create table 
			System.out.println("Creating table \"" + tableName + "\"...");
			Table table = dynamoDB.createTable(createTableRequest);

			//Wait until table is active
			table.waitForActive();
			System.out.println("Table is active!");

			//Add a user
			Item newUser = new Item()
				.withPrimaryKey("username", "donovan")
				.withString("password", "12345");
			PutItemOutcome outcome = table.putItem(newUser);
			System.out.println(outcome);

			//Retrieve a user
			Item user = table.getItem("username", "donovan");
			System.out.println(user);

			//Describe table
			TableDescription tableDesc = dynamoDB.getTable(tableName).describe();
			System.out.println("Table description: " + tableDesc);

		} catch (AmazonServiceException ase) {	
			System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

	private static void init() throws Exception {
        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
				"Cannot load the credentials from the credential profiles file. " +
				"Please make sure that your credentials file is at the correct " +
				"location (~/.aws/credentials), and is in valid format.",
			e);

		}
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		client.setRegion(usWest2);
		dynamoDB = new DynamoDB(client);
    }

	/*
	private static void searchUser(String username) {
		HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
		Condition condition = new Condition().withComparisonOperator(ComparisonOperator.GT.toString())
			.withAttributeValueList(new AttributeValue().withN("2001")); 
		scanFilter.put("year", condition);
		ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
		ScanResult scanResult = dynamoDB.scan(scanRequest);
		System.out.println("Result: " + scanResult);

	}
	*/
}
