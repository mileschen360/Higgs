package restAPIAccess;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.eclipse.jdt.core.dom.ASTVisitor;


import Node.IndexHits;
import Node.NodeJSON;
import RestAPI.GraphServerAccess;
import RestAPI.ThreadedMethodContainerFetch;
import RestAPI.ThreadedMethodReturnFetch;
import RestAPI.ThreadedMethodsInClassFetch;
import RestAPI.ThreadedParentFetch;

import com.google.common.collect.HashMultimap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.json.JSONObject;

class FirstASTVisitor extends ASTVisitor
{
	public GraphServerAccess model;
	public CompilationUnit cu;
	public int cutype;
	public HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache;
	public HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache;
	public HashMap<NodeJSON, NodeJSON> methodContainerCache;
	public HashMap<NodeJSON, NodeJSON> methodReturnCache;
	public HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache;
	public HashMap<String, ArrayList<NodeJSON>> parentNodeCache;
	public HashMap<String, ArrayList<ArrayList<NodeJSON>>> shortClassShortMethodCache;
	int NThreads = 10;


	public HashMultimap<String, String> localMethods; 	//list of localMethods mapped to their corresponding parent classes
	public HashSet<String> localClasses;				//List of local class names

	public HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>> methodReturnTypesMap;
	public HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>> variableTypeMap;//holds variables, fields and method param types
	public HashMultimap<Integer, NodeJSON> printtypes;//holds node start loc and possible types
	public HashMultimap<Integer, NodeJSON> printmethods;//holds node start posns and possible methods they can be
	public HashMap<String, Integer> printTypesMap;//maps node start loc to variable names
	public HashMap<String, Integer> printMethodsMap;//holds node start locs with method names

	public Set<String> importList;
	public Stack<String> classNames; 
	public String superclassname;
	public ArrayList<String> interfaces;
	public int tolerance;
	public int MAX_CARDINALITY;
	public HashMap<String, IndexHits<NodeJSON>> allMethodsInClass;
	public HashSet<String> primitiveTypesSet;
	
	/*public FirstASTVisitor(GraphServerAccess db, CompilationUnit cu, int cutype, int tolerance, int max_cardinality) 
	{
		this.model = db;
		this.cu = cu;
		this.cutype = cutype;
		this.tolerance = tolerance;
		MAX_CARDINALITY = max_cardinality;
		initializeAllFields();
		fetchLocalClassesAndMethods(cu);
	}*/

	public FirstASTVisitor(PrefetchCandidates prefetch_visitor)
	{
		model = prefetch_visitor.model;
		cu = prefetch_visitor.cu;
		cutype = prefetch_visitor.cutype;
		tolerance = prefetch_visitor.tolerance;
		MAX_CARDINALITY = prefetch_visitor.MAX_CARDINALITY;
		localMethods = prefetch_visitor.localMethods;
		localClasses = prefetch_visitor.localClasses;

		candidateClassNodesCache = prefetch_visitor.candidateClassNodesCache;
		candidateMethodNodesCache = prefetch_visitor.candidateMethodNodesCache;
		methodContainerCache = prefetch_visitor.methodContainerCache;
		methodReturnCache = prefetch_visitor.methodReturnCache;
		methodParameterCache = prefetch_visitor.methodParameterCache;
		parentNodeCache = prefetch_visitor.parentNodeCache;
		shortClassShortMethodCache = prefetch_visitor.shortClassShortMethodCache;
		allMethodsInClass = prefetch_visitor.allMethodsInClass;

		classNames = prefetch_visitor.classNames;
		superclassname = prefetch_visitor.superclassname;
		interfaces = prefetch_visitor.interfaces;

		variableTypeMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>();
		methodReturnTypesMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>();
		printtypes = HashMultimap.create();
		printmethods = HashMultimap.create();
		printTypesMap = new HashMap<String, Integer>();
		printMethodsMap = new HashMap<String, Integer>();
		importList = new HashSet<String>();
		
		primitiveTypesSet = prefetch_visitor.primitiveTypesSet;
	}

	/*
	 * Initialize fields in the FirstASTVisitor class
	 */
	private void initializeAllFields()
	{
		localMethods = HashMultimap.create();
		localClasses = new HashSet<String>();
		variableTypeMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>();
		methodReturnTypesMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>();
		printtypes = HashMultimap.create();
		printmethods = HashMultimap.create();
		printTypesMap = new HashMap<String, Integer>();
		printMethodsMap = new HashMap<String, Integer>();
		importList = new HashSet<String>();

		candidateClassNodesCache = new HashMap<String, IndexHits<NodeJSON>>();
		candidateMethodNodesCache = new HashMap<String, IndexHits<NodeJSON>>();
		methodContainerCache = new HashMap<NodeJSON, NodeJSON>();
		methodReturnCache = new HashMap<NodeJSON, NodeJSON>();
		methodParameterCache = new HashMap<NodeJSON, ArrayList<NodeJSON>>();
		parentNodeCache = new HashMap<String, ArrayList<NodeJSON>>();
		shortClassShortMethodCache = new HashMap<String, ArrayList<ArrayList<NodeJSON>>>();
		allMethodsInClass = new HashMap<String, IndexHits<NodeJSON>>();

		importList = new HashSet<String>();
		classNames = new Stack<String>();
		superclassname = new String();
		interfaces = new ArrayList<String>();
	}


	/*
	 * Prints the values in each of the maps stored as fields.
	 */
	public void printFields()
	{
		System.out.println("methodReturnTypesMap: " + methodReturnTypesMap);
		System.out.println("variableTypeMap: " + variableTypeMap);
		System.out.println("printtypes: " + printtypes);
		System.out.println("printmethods: " + printmethods);
		System.out.println("printTypesMap: " + printTypesMap);
		System.out.println("printMethodsMap: " + printMethodsMap);
		System.out.println("possibleImportList: " + importList);
		System.out.println("localMethods: " + localMethods);
	}

	/*
	 * Prunes candidates off a list of candidates based on possible import statements identified so far.
	 * Returns the reduced list (if any changes, else returns the input list as is).
	 */
	
	private HashSet<NodeJSON> getNewClassElementsList(Set<NodeJSON> candidateClassElements)
	{
		HashSet<String> completeSet = new HashSet<String>();
		HashSet<String> prunedSet = new HashSet<String>();
		HashSet<NodeJSON> classElementsHashSet = new HashSet<NodeJSON>();
		HashSet<NodeJSON> prunedClassElementList = new HashSet<NodeJSON>();
		int flag = 0;

		for(NodeJSON classElement: candidateClassElements)
		{
			if (classElement == null) {//added by nmeng
				continue;
			}
			String className = (String) classElement.getProperty("id");
			if(!completeSet.contains(className))
			{
				classElementsHashSet.add(classElement);
				completeSet.add(className);
			}
			if(!importList.isEmpty())
			{
				for(String importItem : importList)
				{
					if(importItem.contains(".*"))
					{
						importItem = importItem.substring(0, importItem.indexOf(".*"));
					}
					if(className.startsWith(importItem) || className.startsWith("java.lang"))
					{
						if(!prunedSet.contains(className))
						{
							prunedSet.add(className);
							prunedClassElementList.add(classElement);
							flag = 1;
						}
					}
				}
			}
		}
		if(flag == 0)
			return classElementsHashSet;
		else
			return prunedClassElementList;
	}
	
	private ArrayList<NodeJSON> getNewClassElementsList(List<NodeJSON> candidateClassElements)
	{
		HashSet<String> completeSet = new HashSet<String>();
		HashSet<String> prunedSet = new HashSet<String>();
		ArrayList<NodeJSON> classElementsArrayList = new ArrayList<NodeJSON>();
		ArrayList<NodeJSON> prunedClassElementList = new ArrayList<NodeJSON>();
		int flag = 0;

		for(NodeJSON classElement: candidateClassElements)
		{
			if (classElement == null) {//added by nmeng
				continue;
			}
			String className = (String) classElement.getProperty("id");
			if(!completeSet.contains(className))
			{
				classElementsArrayList.add(classElement);
				completeSet.add(className);
			}
			if(!importList.isEmpty())
			{
				for(String importItem : importList)
				{
					if(importItem.contains(".*"))
					{
						importItem = importItem.substring(0, importItem.indexOf(".*"));
					}
					if(className.startsWith(importItem))
					{
						if(!prunedSet.contains(className))
						{
							prunedSet.add(className);
							prunedClassElementList.add(classElement);
							flag = 1;
						}
					}
				}
			}
			if(className.startsWith("java.lang"))
			{
				if(!prunedSet.contains(className))
				{
					prunedSet.add(className);
					prunedClassElementList.add(classElement);
					flag = 1;
				}
			}
		}
		if(flag == 0)
			return classElementsArrayList;
		else
			return prunedClassElementList;
	}


	/*
	 * Called when a match with cardinality < Tolerance is found. 
	 * Corresponding import is extracted and added to list of imports.
	 */
	private void addCorrespondingImport(String classID)
	{
		int loc = classID.indexOf('.');
		if(loc != -1)
		{
			String possibleImport = classID.substring(0, classID.lastIndexOf(".")) + ".*";
			importList.add(possibleImport);
			
			//importList.add(classID);
		}
	}


	/*
	 * An array of character positions of parent nodes is extracted and returned to track scope of a statement.
	 */
	private ArrayList<Integer> getScopeArray(ASTNode treeNode)
	{
		ASTNode parentNode;
		ArrayList<Integer> parentList = new ArrayList<Integer>();
		while((parentNode = treeNode.getParent())!=null)
		{
			//do not include parenthesized type nodes in the list.
			if(parentNode.getNodeType() != ASTNode.PARENTHESIZED_EXPRESSION)
				parentList.add(parentNode.getStartPosition());
			treeNode = parentNode;
		}
		return parentList;
	}

	/*
	 * Populates a list of local classes and methods inside these classes into the localClasses and localMethods fields.
	 */
	private void fetchLocalClassesAndMethods(CompilationUnit cu) 
	{
		cu.accept(new ASTVisitor() {
			Stack<String> classNames = new Stack<String>(); 
			public boolean visit(TypeDeclaration treeNode)
			{
				String currentClassName = treeNode.getName().toString(); 
				classNames.push(currentClassName);
				localClasses.add(currentClassName);
				return true;
			}

			public boolean visit(final ClassInstanceCreation treeNode)
			{
				ASTNode anon = treeNode.getAnonymousClassDeclaration();
				if(anon!=null)
				{
					anon.accept(new ASTVisitor(){
						public void endVisit(MethodDeclaration methodNode)
						{
							String className = treeNode.getType().toString();
							String methodName = methodNode.getName().toString();
							localMethods.put(methodName, className);
						}
					});
				}
				return true;
			}

			public void endVisit(TypeDeclaration treeNode)
			{
				classNames.pop();
			}

			public boolean visit(MethodDeclaration treeNode)
			{
				String methodName = treeNode.getName().toString();
				localMethods.put(methodName, classNames.peek());
				return true;
			}
		});
	}

	/*
	 * Checks if a Method called by an Expression is a local method or not.
	 */
	public boolean isLocalMethod(String methodName, Expression expression)
	{
		if(expression == null)
		{
			if(localMethods.containsKey(methodName))
			{
				if(localMethods.get(methodName).contains(classNames.peek()))
					return true;
			}
		}
		else
		{
			if(localMethods.containsKey(methodName))
			{
				if(expression.toString().equals("this"))
					return true;
			}
		}
		return false;
	}

	/*
	 * Checks if a class is a locally defined class.
	 */
	public boolean isLocalClass(String className)
	{
		if(localClasses.contains(className))
			return true;
		else
			return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.VariableDeclarationStatement)
	 * Extracts candidate classes for variables and populates the maps accordingly 
	 */

	//Max Parallel
	public void endVisit(VariableDeclarationStatement treeNode)
	{
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		int startPosition = treeNode.getType().getStartPosition();
		String treeNodeType = treeNode.getType().toString();
		if(treeNode.getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			treeNodeType = ((ParameterizedType)treeNode.getType()).getType().toString();

		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(treeNodeType))
			candidateClassNodes = model.getCandidateClassNodes(treeNodeType, candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);

		for(int j = 0; j < treeNode.fragments().size(); j++)
		{
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator;
			String variableName = ((VariableDeclarationFragment) treeNode.fragments().get(j)).getName().toString();
			printTypesMap.put(variableName, startPosition);
			if(variableTypeMap.containsKey(variableName))
			{
				candidateAccumulator = variableTypeMap.get(variableName);
			}
			else
			{
				candidateAccumulator = HashMultimap.create();
			}

			for(NodeJSON candidateClass : candidateClassNodes)
			{
				candidateAccumulator.put(scopeArray, candidateClass);
				printtypes.put(startPosition, candidateClass);
				if(candidateClassNodes.size() < tolerance)
					addCorrespondingImport(candidateClass.getProperty("id").toString());
			}
			variableTypeMap.put(variableName, candidateAccumulator);
		}
	}

	//Max Parallel
	public boolean visit(EnhancedForStatement treeNode)
	{
		ArrayList<Integer> scopeArray = getScopeArray(treeNode.getParent());
		int startPosition = treeNode.getParameter().getType().getStartPosition();

		String variableType = treeNode.getParameter().getType().toString();
		if(treeNode.getParameter().getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			variableType = ((ParameterizedType)treeNode.getParameter().getType()).getType().toString();

		String variableName = treeNode.getParameter().getName().toString();

		HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator;
		if(variableTypeMap.containsKey(treeNode.getParameter().getName().toString()))
			candidateAccumulator = variableTypeMap.get(variableName);
		else
			candidateAccumulator = HashMultimap.create();
		
		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(variableType))
			candidateClassNodes=model.getCandidateClassNodes(variableType, candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);

		printTypesMap.put(variableName, startPosition);
		for(NodeJSON candidateClass : candidateClassNodes)
		{
			candidateAccumulator.put(scopeArray, candidateClass);
			printtypes.put(startPosition, candidateClass);
			if(candidateClassNodes.size() < tolerance)
				addCorrespondingImport(candidateClass.getProperty("id").toString());
		}
		variableTypeMap.put(variableName, candidateAccumulator);
		return true;
	}

	//Max Parallel
	public void endVisit(FieldDeclaration treeNode) 
	{
		
		int startPosition = treeNode.getType().getStartPosition();
		ArrayList<Integer> variableScopeArray = getScopeArray(treeNode);

		String fieldType = null;
		if(treeNode.getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			fieldType = ((ParameterizedType)treeNode.getType()).getType().toString();
		else
			fieldType = treeNode.getType().toString();

		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(fieldType))
			candidateClassNodes = model.getCandidateClassNodes(fieldType, candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);

		
		for(int j=0; j < treeNode.fragments().size(); j++)
		{
			String fieldName = ((VariableDeclarationFragment)treeNode.fragments().get(j)).getName().toString();
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
			if(variableTypeMap.containsKey(fieldName))
				candidateAccumulator = variableTypeMap.get(fieldName);
			else
				candidateAccumulator = HashMultimap.create();

			printTypesMap.put(fieldName, startPosition);
			for(NodeJSON candidateClass : candidateClassNodes)
			{
				candidateAccumulator.put(variableScopeArray, candidateClass);
				printtypes.put(startPosition, candidateClass);
				if(candidateClassNodes.size() < tolerance)
					addCorrespondingImport(candidateClass.getProperty("id").toString());
			}
			variableTypeMap.put(fieldName, candidateAccumulator);
		}
	}

	//Max Parallel
	@SuppressWarnings("unchecked")
	public void endVisit(MethodInvocation treeNode)
	{
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		Expression expression = treeNode.getExpression();
		String treeNodeMethodExactName = treeNode.getName().toString();
		String treeNodeString = treeNode.toString();
		int startPosition = treeNode.getName().getStartPosition();

		String expressionString = null;
		if(isLocalMethod(treeNodeMethodExactName, expression) == true)
			return;

		printTypesMap.put(treeNodeString, startPosition);
		printMethodsMap.put(treeNodeString, startPosition);

		if(expression != null)
		{
			expressionString = expression.toString();
			if(expressionString.startsWith("(") && expressionString.endsWith(")"))
				expressionString = expressionString.substring(1, expressionString.length()-1);
		}
		if(expression == null)
		{
			//Max Parallel
			if(superclassname != null)
			{
				/*
				 * Handles inheritance, where methods from Superclasses can be directly called
				 */


				HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
				if(methodReturnTypesMap.containsKey(treeNodeString))
					candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
				else
					candidateAccumulator = HashMultimap.create();
				ArrayList<NodeJSON> candidateSuperClassNodes = new ArrayList<NodeJSON>();
				if(!isLocalClass(superclassname))
					candidateSuperClassNodes = model.getCandidateClassNodes(superclassname, candidateClassNodesCache);

				candidateSuperClassNodes = getNewClassElementsList(candidateSuperClassNodes);

				List<NodeJSON> replacementClassNodesList = Collections.synchronizedList(new ArrayList<NodeJSON>());
				List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
				List<NodeJSON> candidateParentNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());

				ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);
				ExecutorService getParentClass = Executors.newFixedThreadPool(NThreads);

				for(NodeJSON candidateSuperClassNode : candidateSuperClassNodes)
				{
					ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateSuperClassNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
					getMethodsInClass.execute(tmicf);

					if(!isPrimitive(candidateSuperClassNode.getProperty("id")))
					{
						ThreadedParentFetch tpf = new ThreadedParentFetch(candidateSuperClassNode, treeNode, candidateParentNodes, parentNodeCache, model);
						getParentClass.execute(tpf);
					}

				}
				getMethodsInClass.shutdown();
				while(getMethodsInClass.isTerminated() == false)
				{
				}
				int hasCandidateFlag = 0;
				ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
				ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
				for(NodeJSON candidateMethodNode : candidateMethodNodes)
				{
					String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
					if((candidateMethodExactName).equals( treeNode.getName().toString()))
					{
						if(matchParams(candidateMethodNode, treeNode.arguments())==true)
						{
							printmethods.put(startPosition, candidateMethodNode);

							ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
							getMethodContainerExecutor.execute(tmcf);

							ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model, treeNode);
							getMethodReturnExecutor.execute(tmrf);

							hasCandidateFlag = 1;
						}
					}
				}
				
				getParentClass.shutdown();
				getMethodReturnExecutor.shutdown();
				getMethodContainerExecutor.shutdown();
				while(getMethodReturnExecutor.isTerminated() == false || getMethodContainerExecutor.isTerminated() == false || getParentClass.isTerminated() == false)
				{
				}
				candidateParentNodes = getNewClassElementsList(candidateParentNodes);
				if(hasCandidateFlag == 0)
				{
					getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
					getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
					candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
					for(NodeJSON parentNode: candidateParentNodes)
					{
						ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(parentNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
						getMethodsInClass.execute(tmicf);
					}
					getMethodsInClass.shutdown();
					while(getMethodsInClass.isTerminated() == false)
					{
					}

					for(NodeJSON candidateMethodNode : candidateMethodNodes)
					{
						String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
						if(candidateMethodExactName.equals(treeNode.getName().toString()))
						{
							if(matchParams(candidateMethodNode, treeNode.arguments())==true)
							{
								printmethods.put(startPosition, candidateMethodNode);

								ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
								getMethodContainerExecutor.execute(tmcf);

								ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
								getMethodReturnExecutor.execute(tmrf);
							}
						}
					}
					getMethodReturnExecutor.shutdown();
					getMethodContainerExecutor.shutdown();
					while(getMethodReturnExecutor.isTerminated() == false || getMethodContainerExecutor.isTerminated() == false)
					{
					}
				}

				methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
				printtypes.replaceValues(startPosition, replacementClassNodesList);
				if(replacementClassNodesList.size() < tolerance)
				{
					for(NodeJSON candidate : replacementClassNodesList)
						addCorrespondingImport(candidate.getProperty("id").toString());
				}
			}
			//Max Parallel
			else
			{
				/*
				 * Might be user declared helper functions or maybe object reference is assumed to be obvious in the snippet
				 */
				HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
				if(methodReturnTypesMap.containsKey(treeNodeString))
				{
					candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
				}
				else
				{
					candidateAccumulator = HashMultimap.create();
				}
				ArrayList<NodeJSON> candidateMethodNodes = model.getCandidateMethodNodes(treeNode.getName().toString(), candidateMethodNodesCache);
				ArrayList<NodeJSON> replacementClassNodesList = new ArrayList<NodeJSON>();
				ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
				ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
				for(NodeJSON candidateMethodNode : candidateMethodNodes)
				{
					if(matchParams(candidateMethodNode, treeNode.arguments())==true)
					{
						printmethods.put(startPosition, candidateMethodNode);

						ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
						getMethodContainerExecutor.execute(tmcf);

						ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
						getMethodReturnExecutor.execute(tmrf);
					}
				}
				getMethodContainerExecutor.shutdown();
				getMethodReturnExecutor.shutdown();
				while(getMethodContainerExecutor.isTerminated() == false || getMethodReturnExecutor.isTerminated()==false)
				{

				}
				methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
				printtypes.replaceValues(treeNode.getStartPosition(), replacementClassNodesList);
				if(replacementClassNodesList.size() < tolerance)
				{
					for(NodeJSON candidate : replacementClassNodesList)
						addCorrespondingImport(candidate.getProperty("id").toString());
				}	
			}
		}
		else if(expressionString.contains("System."))
		{

		}
		else if(expression.getNodeType() == ASTNode.ARRAY_ACCESS)
		{
			//System.out.println("array method");
		}
		//Max Parallel
		else if(expression.getNodeType() == ASTNode.FIELD_ACCESS)
		{
			FieldAccess fe = (FieldAccess)expression;
			if(fe.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION)
			{
				expressionString = fe.getName().toString();
				List<NodeJSON> replacementClassNodesList = Collections.synchronizedList(new ArrayList<NodeJSON>());
				
				HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap = null;
				if(variableTypeMap.containsKey(expressionString))
					temporaryMap = variableTypeMap.get(expressionString);
				else
					temporaryMap = HashMultimap.create();
				ArrayList<Integer> rightScopeArray = getNodeSet(temporaryMap, scopeArray);
				if(rightScopeArray == null)
					return;
				Set<NodeJSON> candidateClassNodes = temporaryMap.get(rightScopeArray);
				candidateClassNodes = getNewClassElementsList(candidateClassNodes);

				HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
				if(methodReturnTypesMap.containsKey(treeNodeString))
				{
					candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
				}
				else
				{
					candidateAccumulator = HashMultimap.create();
				}

				List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
				List<NodeJSON> candidateParentNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());

				ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);
				ExecutorService getParentClass = Executors.newFixedThreadPool(NThreads);

				for(NodeJSON candidateClassNode : candidateClassNodes)
				{
					ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
					getMethodsInClass.execute(tmicf);

					if(!isPrimitive(candidateClassNode.getProperty("id")))
					{
						ThreadedParentFetch tpf = new ThreadedParentFetch(candidateClassNode, treeNode, candidateParentNodes, parentNodeCache, model);
						getParentClass.execute(tpf);
					}
				}
				getMethodsInClass.shutdown();
				while(getMethodsInClass.isTerminated() == false)
				{
				}

				int hasCandidateFlag = 0;
				ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
				ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
				for(NodeJSON candidateMethodNode : candidateMethodNodes)
				{
					String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
					if((candidateMethodExactName).equals( treeNode.getName().toString()))
					{
						if(matchParams(candidateMethodNode, treeNode.arguments())==true)
						{
							printmethods.put(startPosition, candidateMethodNode);

							ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
							getMethodContainerExecutor.execute(tmcf);

							ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
							getMethodReturnExecutor.execute(tmrf);

							//hasCandidateFlag = 1;
						}
					}
				}

				getParentClass.shutdown();
				getMethodReturnExecutor.shutdown();
				getMethodContainerExecutor.shutdown();
				while(getMethodReturnExecutor.isTerminated() == false || getMethodContainerExecutor.isTerminated() == false || getParentClass.isTerminated() == false)
				{

				}
				candidateParentNodes = getNewClassElementsList(candidateParentNodes);
				if(hasCandidateFlag == 0)
				{
					getMethodsInClass = Executors.newFixedThreadPool(NThreads);
					getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
					getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
					candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
					for(NodeJSON parentNode: candidateParentNodes)
					{
						ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(parentNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
						getMethodsInClass.execute(tmicf);
					}
					getMethodsInClass.shutdown();
					while(getMethodsInClass.isTerminated() == false)
					{
					}

					for(NodeJSON candidateMethodNode : candidateMethodNodes)
					{
						String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
						if(candidateMethodExactName.equals(treeNode.getName().toString()))
						{
							if(matchParams(candidateMethodNode, treeNode.arguments())==true)
							{
								printmethods.put(startPosition, candidateMethodNode);

								ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
								getMethodContainerExecutor.execute(tmcf);

								ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
								getMethodReturnExecutor.execute(tmrf);
							}
						}
					}
					getMethodReturnExecutor.shutdown();
					getMethodContainerExecutor.shutdown();
					while(getMethodReturnExecutor.isTerminated() == false || getMethodContainerExecutor.isTerminated() == false)
					{
					}
				}

				methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
				printtypes.replaceValues(startPosition, replacementClassNodesList);
				if(replacementClassNodesList.size()!=0)
				{
					temporaryMap.replaceValues(rightScopeArray, replacementClassNodesList);
					variableTypeMap.put(expressionString, temporaryMap);
					printtypes.replaceValues(printTypesMap.get(expressionString), replacementClassNodesList);
				}
				if(replacementClassNodesList.size() < tolerance)
				{
					for(NodeJSON candidate : replacementClassNodesList)
						addCorrespondingImport(candidate.getProperty("id").toString());
				}
			}

		}
		else if(variableTypeMap.containsKey(expressionString))
		{
			List<NodeJSON> replacementClassNodesList = Collections.synchronizedList(new ArrayList<NodeJSON>());
			HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap = variableTypeMap.get(expressionString);
			ArrayList<Integer> rightScopeArray = getNodeSet(temporaryMap, scopeArray);
			if(rightScopeArray == null)
				return;
			Set<NodeJSON> candidateClassNodes = temporaryMap.get(rightScopeArray);
			candidateClassNodes = getNewClassElementsList(candidateClassNodes);
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
			if(methodReturnTypesMap.containsKey(treeNodeString))
			{
				candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
			}
			else
			{
				candidateAccumulator = HashMultimap.create();
			}
			List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
			List<NodeJSON> candidateParentNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());

			ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);
			ExecutorService getParentClass = Executors.newFixedThreadPool(NThreads);

			for(NodeJSON candidateClassNode : candidateClassNodes)
			{
				ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
				getMethodsInClass.execute(tmicf);

				if(!isPrimitive(candidateClassNode.getProperty("id")))
				{
					ThreadedParentFetch tpf = new ThreadedParentFetch(candidateClassNode, treeNode, candidateParentNodes, parentNodeCache, model);
					getParentClass.execute(tpf);
				}
			}
			getMethodsInClass.shutdown();
			while(getMethodsInClass.isTerminated() == false)
			{
			}

			int hasCandidateFlag = 0;
			ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
			ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
			for(NodeJSON candidateMethodNode : candidateMethodNodes)
			{
				String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
				if((candidateMethodExactName).equals( treeNode.getName().toString()))
				{
					if(matchParams(candidateMethodNode, treeNode.arguments())==true)
					{
						printmethods.put(startPosition, candidateMethodNode);
						ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
						getMethodContainerExecutor.execute(tmcf);

						ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
						getMethodReturnExecutor.execute(tmrf);

						//hasCandidateFlag = 1;
					}
				}
			}

			getParentClass.shutdown();
			getMethodReturnExecutor.shutdown();
			getMethodContainerExecutor.shutdown();
			while(getMethodReturnExecutor.isTerminated() == false || getMethodContainerExecutor.isTerminated() == false || getParentClass.isTerminated() == false)
			{

			}
			candidateParentNodes = getNewClassElementsList(candidateParentNodes);
			if(hasCandidateFlag == 0)
			{
				getMethodsInClass = Executors.newFixedThreadPool(NThreads);
				getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
				getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
				candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
				for(NodeJSON parentNode: candidateParentNodes)
				{
					ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(parentNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
					getMethodsInClass.execute(tmicf);
				}
				getMethodsInClass.shutdown();
				while(getMethodsInClass.isTerminated() == false)
				{
				}

				for(NodeJSON candidateMethodNode : candidateMethodNodes)
				{
					String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
					if(candidateMethodExactName.equals(treeNode.getName().toString()))
					{
						if(matchParams(candidateMethodNode, treeNode.arguments())==true)
						{
							printmethods.put(startPosition, candidateMethodNode);
							ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
							getMethodContainerExecutor.execute(tmcf);

							ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
							getMethodReturnExecutor.execute(tmrf);
						}
					}
				}
				getMethodReturnExecutor.shutdown();
				getMethodContainerExecutor.shutdown();
				while(getMethodReturnExecutor.isTerminated() == false || getMethodContainerExecutor.isTerminated() == false)
				{
				}
			}

			methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
			printtypes.putAll(startPosition, replacementClassNodesList);
			if(replacementClassNodesList.size()!=0)
			{
				temporaryMap.replaceValues(rightScopeArray, replacementClassNodesList);
				variableTypeMap.put(expressionString, temporaryMap);
				printtypes.replaceValues(printTypesMap.get(expressionString), replacementClassNodesList);
			}
			
			if(replacementClassNodesList.size() < tolerance)
			{
				for(NodeJSON candidate : replacementClassNodesList)
					addCorrespondingImport(candidate.getProperty("id").toString());
			}
		}
		else if(expressionString.matches("[A-Z][a-zA-Z]*"))
		{
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
			if(methodReturnTypesMap.containsKey(treeNodeString))
			{
				candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
			}
			else
			{
				candidateAccumulator = HashMultimap.create();
			}
			List <NodeJSON> replacementClassNodesList = Collections.synchronizedList(new ArrayList<NodeJSON>());

			ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
			ArrayList<NodeJSON> candidateMethodNodes = new ArrayList<NodeJSON>();
			ArrayList<NodeJSON> candidateReturnNodes = new ArrayList<NodeJSON>();

			if(!isLocalClass(expressionString))
			{
				ArrayList<ArrayList<NodeJSON>> tempArrayList = model.getMethodNodeWithShortClass(treeNodeMethodExactName, expressionString, shortClassShortMethodCache, methodReturnCache, methodContainerCache);
				candidateClassNodes = tempArrayList.get(0);
				candidateMethodNodes = tempArrayList.get(1);
				candidateReturnNodes = tempArrayList.get(2);
			}

			ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
			for(int i=0; i<candidateMethodNodes.size(); i++)
			{
				NodeJSON candidateMethodNode = candidateMethodNodes.get(i);
				//System.out.println(candidateMethodNode.getProperty("id") + treeNode.arguments().size());
				if(matchParams(candidateMethodNode, treeNode.arguments())==true)
				{
					printmethods.put(startPosition, candidateMethodNode);
					replacementClassNodesList.add(candidateClassNodes.get(i));
					//ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model);
					//getMethodReturnExecutor.execute(tmrf);
					candidateAccumulator.putAll(scopeArray, candidateReturnNodes);
				}
			}
			getMethodReturnExecutor.shutdown();
			while(!getMethodReturnExecutor.isTerminated())
			{

			}
			methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
			printTypesMap.put(expressionString, expression.getStartPosition());
			HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap = HashMultimap.create();
			
			if(replacementClassNodesList.size()!=0)
			{
				ArrayList<Integer> topLevelScope = new ArrayList<Integer>();
				topLevelScope.add(0);
				temporaryMap.replaceValues(topLevelScope, replacementClassNodesList);
				variableTypeMap.put(expressionString, temporaryMap);
				printtypes.replaceValues(printTypesMap.get(expressionString), replacementClassNodesList);
			}
			
			if(replacementClassNodesList.size() < tolerance)
			{
				for(NodeJSON candidate : replacementClassNodesList)
					addCorrespondingImport(candidate.getProperty("id").toString());
			}
		}

		else if(methodReturnTypesMap.containsKey(expressionString))
		{
			HashMultimap<ArrayList<Integer>, NodeJSON> nodeInMap = methodReturnTypesMap.get(expressionString);
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
			if(methodReturnTypesMap.containsKey(treeNodeString))
			{
				candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
			}
			else
			{
				candidateAccumulator = HashMultimap.create();
			}
			
			List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
			List<NodeJSON> candidateParentNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
			List<NodeJSON> replacementClassNodesList = Collections.synchronizedList(new ArrayList<NodeJSON>());
			
			ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);
			ExecutorService getParentClass = Executors.newFixedThreadPool(NThreads);

			ArrayList<Integer> newscopeArray = getNodeSet(nodeInMap, scopeArray);
			Set<NodeJSON> candidateClassNodes = nodeInMap.get(newscopeArray);
			candidateClassNodes = getNewClassElementsList(candidateClassNodes);
			for(NodeJSON candidateClassNode : candidateClassNodes)
			{
				ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
				getMethodsInClass.execute(tmicf);

				if(!isPrimitive(candidateClassNode.getProperty("id")))
				{
					ThreadedParentFetch tpf = new ThreadedParentFetch(candidateClassNode, treeNode, candidateParentNodes, parentNodeCache, model);
					getParentClass.execute(tpf);
				}
			}
			getMethodsInClass.shutdown();
			while(getMethodsInClass.isTerminated() == false)
			{
			}
			
			ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
			ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
			
			for(NodeJSON candidateMethodNode : candidateMethodNodes)
			{
				String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
				if(candidateMethodExactName.equals(treeNodeMethodExactName))
				{
					if(matchParams(candidateMethodNode, treeNode.arguments())==true)
					{
						printmethods.put(startPosition, candidateMethodNode);
						
						ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
						getMethodReturnExecutor.execute(tmrf);
						
						ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
						getMethodContainerExecutor.execute(tmcf);

					}
				}
			}
			getParentClass.shutdown();
			getMethodReturnExecutor.shutdown();
			getMethodContainerExecutor.shutdown();
			while(getMethodReturnExecutor.isTerminated() == false || getMethodContainerExecutor.isTerminated() == false || getParentClass.isTerminated() == false)
			{

			}
			candidateParentNodes = getNewClassElementsList(candidateParentNodes);
			///
			methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
			if(replacementClassNodesList.size()!=0)
			{
				printtypes.replaceValues(printTypesMap.get(expressionString), replacementClassNodesList);
				HashMultimap<ArrayList<Integer>, NodeJSON> replacer = HashMultimap.create();
				replacer.putAll(newscopeArray, replacementClassNodesList);
				methodReturnTypesMap.put(expressionString, replacer);
			}
			
		}
		else
		{
			ArrayList <NodeJSON> replacementClassNodesList= new ArrayList<NodeJSON>();
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
			if(methodReturnTypesMap.containsKey(treeNodeString))
			{
				candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
			}
			else
			{
				candidateAccumulator = HashMultimap.create();
			}

			printMethodsMap.put(treeNodeString, startPosition);
			ArrayList<NodeJSON> candidateMethodNodes = model.getCandidateMethodNodes(treeNodeMethodExactName, candidateMethodNodesCache);
			ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
			ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
			for(NodeJSON candidateMethodNode : candidateMethodNodes)
			{
				if(matchParams(candidateMethodNode, treeNode.arguments())==true)
				{
					printmethods.put(startPosition, candidateMethodNode);

					ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, replacementClassNodesList, model);
					getMethodContainerExecutor.execute(tmcf);

					ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model,treeNode);
					getMethodReturnExecutor.execute(tmrf);
				}
			}
			getMethodContainerExecutor.shutdown();
			getMethodReturnExecutor.shutdown();
			while(getMethodContainerExecutor.isTerminated() == false || getMethodReturnExecutor.isTerminated()==false)
			{

			}
			methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
			printtypes.replaceValues(startPosition, replacementClassNodesList);
			printTypesMap.put(expressionString, startPosition);
			if(replacementClassNodesList.isEmpty()==false)
			{
				if(replacementClassNodesList.size()!=0)
				{
					HashMultimap<ArrayList<Integer>, NodeJSON> replacer = HashMultimap.create();
					//Since we are not sure where this variable could have be declared, we assume it should have been a field
					ArrayList<Integer> fieldScope = new ArrayList<Integer>();
					fieldScope.add(0);
					replacer.putAll(fieldScope, replacementClassNodesList);
					variableTypeMap.put(expressionString, replacer);
				}
			}
		}
	}

	//Max parallel
	private ArrayList<Integer> getNodeSet(HashMultimap<ArrayList<Integer>, NodeJSON> celist2, ArrayList<Integer> scopeArray) 
	{
		for(ArrayList<Integer> test : celist2.keySet())
		{
			if(isSubset(test, scopeArray))
				return test;
		}
		return null;
	}

	//Max parallel
	private boolean isSubset(ArrayList<Integer> test,ArrayList<Integer> scopeArray) 
	{
		if(scopeArray.containsAll(test.subList(1, test.size())))
			return true;
		/*else if(scopeArray.containsAll(test.subList(1, test.size())))
			return true;*/
		else
			return false;
	}

	//Max parallel
	private boolean matchParams(NodeJSON me, List<ASTNode> params) 
	{
		ArrayList<HashSet<String>> nodeArgs = new ArrayList<HashSet<String>>();
		ArrayList<NodeJSON> graphNodes = new ArrayList<NodeJSON>();
		//if(graphNodes == null)
		graphNodes = model.getMethodParams(me, methodParameterCache);

		if(graphNodes.size() != params.size())
		{
			return false;
		}
		if(params.size()==0 && graphNodes.size()==0)
		{
			return true;
		}
		for(int i = 0; i< params.size(); i++)
		{
			ASTNode param = params.get(i);
			HashSet<String> possibleTypes = new HashSet<String>();
			if(param.getNodeType() == ASTNode.NUMBER_LITERAL)
			{
				possibleTypes.add("int");
				possibleTypes.add("byte");
				possibleTypes.add("float");
				possibleTypes.add("double");
				possibleTypes.add("long");
				possibleTypes.add("short");
			}
			else if(param.getNodeType() == ASTNode.BOOLEAN_LITERAL)
			{
				possibleTypes.add("boolean");
			}
			else if(param.getNodeType() == ASTNode.CHARACTER_LITERAL)
			{
				possibleTypes.add("char");
			}
			else if(param.getNodeType() == ASTNode.INFIX_EXPRESSION)
			{
				InfixExpression tempNode = (InfixExpression) param;
				if(tempNode.getLeftOperand().getNodeType() == ASTNode.STRING_LITERAL || tempNode.getRightOperand().getNodeType() == ASTNode.STRING_LITERAL)
					possibleTypes.add("String");
				else if(tempNode.getLeftOperand().getNodeType() == ASTNode.NUMBER_LITERAL || tempNode.getRightOperand().getNodeType() == ASTNode.NUMBER_LITERAL)
				{
					possibleTypes.add("int");
					possibleTypes.add("byte");
					possibleTypes.add("float");
					possibleTypes.add("double");
					possibleTypes.add("long");
					possibleTypes.add("short");
				}
				else
					possibleTypes.add("UNKNOWN");
			}
			else if(param.getNodeType() == ASTNode.STRING_LITERAL)
			{
				possibleTypes.add("String");
				possibleTypes.add("CharSequence");
			}
			else if (param.getNodeType() == ASTNode.SIMPLE_NAME)
			{
				if(variableTypeMap.containsKey(param.toString()))
				{
					HashMultimap<ArrayList<Integer>, NodeJSON> celist_temp = variableTypeMap.get(param.toString());
					ArrayList<Integer> intermediate = getNodeSet(celist_temp, getScopeArray(param));
					if(intermediate!=null)
					{
						Set<NodeJSON> localTypes = celist_temp.get(intermediate);
						for(NodeJSON localType : localTypes)
						{
							possibleTypes.add((String) localType.getProperty("id"));
						}
					}
				}
				else
				{
					possibleTypes.add("UNKNOWN");
				}
			}
			else if(param.getNodeType() == ASTNode.METHOD_INVOCATION)
			{
				if(methodReturnTypesMap.containsKey(param.toString()))
				{
					HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap = methodReturnTypesMap.get(param.toString());
					ArrayList<Integer> scopeArray = getScopeArray(param);
					ArrayList<Integer> rightScopeArray = getNodeSet(temporaryMap, scopeArray);
					Set<NodeJSON> candidateClassNodes = new HashSet<NodeJSON>();
					if(rightScopeArray != null)
						candidateClassNodes = temporaryMap.get(rightScopeArray);
					for(NodeJSON localType : candidateClassNodes)
					{
						possibleTypes.add((String) localType.getProperty("id"));
					}
				}
				else
				{
					possibleTypes.add("UNKNOWN");
				}
			}
			else if(param.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
			{
				ClassInstanceCreation tempNode = (ClassInstanceCreation) param;
				//possibleTypes.add(tempNode.getType().toString());
				HashMultimap<ArrayList<Integer>, NodeJSON> tempMap = methodReturnTypesMap.get(tempNode.toString());
				for(NodeJSON val : tempMap.get(getScopeArray(tempNode)))
					possibleTypes.add(val.getProperty("id"));
			}
			else
			{
				possibleTypes.add("UNKNOWN");
			}
			nodeArgs.add(possibleTypes);
		}


		Iterator<NodeJSON> iter1 = graphNodes.iterator();
		Iterator<HashSet<String>> iter2 = nodeArgs.iterator();
		while(iter1.hasNext())
		{
			NodeJSON graphParam = iter1.next();
			HashSet<String> args = iter2.next();
			int flag=0;
			for(String arg : args)
			{
				if(((String)graphParam.getProperty("exactName")).equals(arg) || ((String)graphParam.getProperty("id")).equals(arg))
				{
					flag = 0;
					break;
				}
				else if(((String)graphParam.getProperty("exactName")).endsWith("." + arg) || ((String)graphParam.getProperty("exactName")).endsWith("$" + arg))
				{
					flag = 0;
					break;
				}
				else if(arg.equals("UNKNOWN"))
				{
					flag = 0;
					break;
				}
				else if(!isPrimitive(arg))
				{
					if(!isPrimitive(graphParam.getProperty("id")))
					{
						if(model.checkIfParentNode(graphParam, arg, parentNodeCache))
						{
							flag = 0;
							break;
						}
						else
						{
							flag = 1;
						}
					}
					else
					{
						flag = 1;
					}
				}
				else
					flag=1;
			}
			if(flag == 1)
				return false;
		}

		return true;
	}

	private boolean isPrimitive(String arg) 
	{
		if(primitiveTypesSet.contains(arg))
			return true;
		return false;
	}

	//Max parallel
	public boolean visit(TypeDeclaration treeNode)
	{
		classNames.push(treeNode.getName().toString());
		if(treeNode.getSuperclassType()!=null)
		{
			if(treeNode.getSuperclassType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			{
				superclassname = ((ParameterizedType)treeNode.getSuperclassType()).getType().toString();
			}
			else
			{
				superclassname = treeNode.getSuperclassType().toString();
			}
		}

		for(Object ob : treeNode.superInterfaceTypes())
		{	
			List<Type> superInterfaces = treeNode.superInterfaceTypes();
			for(Type superInterface : superInterfaces)
			{
				if(superInterface.isParameterizedType())
				{
					interfaces.add(((ParameterizedType)superInterface).getType().toString());
				}
				else if(superInterface.isSimpleType())
				{
					interfaces.add(((SimpleType)superInterface).getName().toString());
				}
				else if(superInterface.isArrayType())
				{
					interfaces.add(((ArrayType)superInterface).getElementType().toString());
				}
				else if(superInterface.isArrayType())
				{
					interfaces.add(((ArrayType)superInterface).getElementType().toString());
				}
			}
		}
		return true;
	}

	//Max parallel
	public void endVisit(TypeDeclaration treeNode)
	{
		classNames.pop();
		superclassname = "";
		interfaces.clear();
	}

	//Max parallel
	public boolean visit(MethodDeclaration treeNode)
	{
		int startPosition = treeNode.getStartPosition();
		List<SingleVariableDeclaration> param = treeNode.parameters();
		for(int i=0;i<param.size();i++)
		{
			ArrayList<Integer> scopeArray = getScopeArray(treeNode);
			HashMultimap<ArrayList<Integer>,NodeJSON> temporaryMap = null;
			if(variableTypeMap.containsKey(param.get(i).getName().toString()))
			{
				temporaryMap = variableTypeMap.get(param.get(i).getName().toString());
			}
			else
			{
				temporaryMap = HashMultimap.create();
			}

			String parameterType = null;
			if(param.get(i).getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
				parameterType = ((ParameterizedType)param.get(i).getType()).getType().toString();
			else
				parameterType = param.get(i).getType().toString();

			ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
			if(!isLocalClass(parameterType))
				candidateClassNodes = model.getCandidateClassNodes(parameterType, candidateClassNodesCache);
			candidateClassNodes = getNewClassElementsList(candidateClassNodes);

			for(NodeJSON candidateClassNode : candidateClassNodes)
			{
				temporaryMap.put(scopeArray, candidateClassNode);
				if(candidateClassNodes.size() < tolerance)
					addCorrespondingImport(candidateClassNode.getProperty("id").toString());
				printtypes.put(param.get(i).getType().getStartPosition(),candidateClassNode);
				printTypesMap.put(param.get(i).getName().toString(), param.get(i).getType().getStartPosition());
			}
			variableTypeMap.put(param.get(i).getName().toString(), temporaryMap);
		}

		if(superclassname!=null)
		{
			ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
			if(!isLocalClass(superclassname))
				candidateClassNodes = model.getCandidateClassNodes(superclassname, candidateClassNodesCache);
			candidateClassNodes = getNewClassElementsList(candidateClassNodes);

			List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
			ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);

			for(NodeJSON candidateClassNode : candidateClassNodes)
			{
				ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
				getMethodsInClass.execute(tmicf);
			}
			getMethodsInClass.shutdown();
			while(getMethodsInClass.isTerminated() == false)
			{
			}

			ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
			List<NodeJSON> methodContainerList = Collections.synchronizedList(new ArrayList<NodeJSON>()); 


			for(NodeJSON candidateMethodNode : candidateMethodNodes)
			{
				String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
				if((candidateMethodExactName).equals( treeNode.getName().toString()))
				{
					if(matchParams(candidateMethodNode, treeNode.parameters())==true)
					{
						printmethods.put(startPosition, candidateMethodNode);
						ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, methodContainerList, model);
						getMethodContainerExecutor.execute(tmcf);
					}
				}
			}
			getMethodContainerExecutor.shutdown();
			while(getMethodContainerExecutor.isTerminated() == false)
			{

			}
			for(NodeJSON methodContainer : methodContainerList)
			{
				printtypes.put(startPosition, methodContainer);
			}
		}

		if(!interfaces.isEmpty())
		{
			for(int i=0;i<interfaces.size();i++)
			{
				ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
				if(!isLocalClass(interfaces.get(i).toString()))
					candidateClassNodes = model.getCandidateClassNodes(interfaces.get(i).toString(), candidateClassNodesCache);
				candidateClassNodes = getNewClassElementsList(candidateClassNodes);

				List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
				ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);

				for(NodeJSON candidateClassNode : candidateClassNodes)
				{
					ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
					getMethodsInClass.execute(tmicf);
				}
				getMethodsInClass.shutdown();
				while(getMethodsInClass.isTerminated() == false)
				{
				}
				ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
				List<NodeJSON> methodContainerList = Collections.synchronizedList(new ArrayList<NodeJSON>()); 
				for(NodeJSON candidateMethodNode : candidateMethodNodes)
				{
					String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
					if((candidateMethodExactName).equals( treeNode.getName().toString()))
					{
						if(matchParams(candidateMethodNode, treeNode.parameters())==true)
						{
							printmethods.put(startPosition, candidateMethodNode);
							ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, methodContainerList, model);
							getMethodContainerExecutor.execute(tmcf);
						}
					}
				}
				getMethodContainerExecutor.shutdown();
				while(getMethodContainerExecutor.isTerminated() == false)
				{

				}
				for(NodeJSON methodContainer : methodContainerList)
				{
					printtypes.put(startPosition, methodContainer);
				}
			}
		}
		return true;
	}

	//Max parallel
	public void endVisit(ConstructorInvocation treeNode)
	{	
		String treeNodeString = treeNode.toString();
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
		if(methodReturnTypesMap.containsKey(treeNodeString))
		{
			candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
		}
		else
		{
			candidateAccumulator = HashMultimap.create();
		}
		int startPosition = treeNode.getStartPosition();
		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(classNames.peek()))
			candidateClassNodes = model.getCandidateClassNodes(classNames.peek(), candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);


		List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
		ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);

		for(NodeJSON candidateClassNode : candidateClassNodes)
		{
			ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, "<init>", candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
			getMethodsInClass.execute(tmicf);
		}
		getMethodsInClass.shutdown();
		while(getMethodsInClass.isTerminated() == false)
		{
		}
		ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
		List<NodeJSON> methodContainerList = Collections.synchronizedList(new ArrayList<NodeJSON>()); 
		for(NodeJSON candidateMethodNode : candidateMethodNodes)
		{
			String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
			if((candidateMethodExactName).equals("<init>"))
			{
				if(matchParams(candidateMethodNode, treeNode.arguments())==true)
				{
					printmethods.put(startPosition, candidateMethodNode);
					ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, methodContainerList, model);
					getMethodContainerExecutor.execute(tmcf);
				}
			}
		}
		getMethodContainerExecutor.shutdown();
		while(getMethodContainerExecutor.isTerminated() == false)
		{

		}
		for(NodeJSON methodContainer : methodContainerList)
		{
			printtypes.put(startPosition, methodContainer);
			candidateAccumulator.put(scopeArray, methodContainer);
		}
		methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
	}

	//Max parallel
	public boolean visit(CatchClause node)
	{
		int startPosition = node.getException().getType().getStartPosition();
		ArrayList<Integer> scopeArray = getScopeArray(node);

		HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap = null;
		if(variableTypeMap.containsKey(node.getException().getName().toString()))
		{
			temporaryMap = variableTypeMap.get(node.getException().getName().toString());
		}
		else
		{
			temporaryMap = HashMultimap.create();
		}
		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(node.getException().getType().toString()))
			candidateClassNodes = model.getCandidateClassNodes(node.getException().getType().toString(), candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);
		for(NodeJSON candidateClassNode : candidateClassNodes)
		{
			temporaryMap.put(scopeArray, candidateClassNode);
			if(candidateClassNodes.size() < tolerance)
				addCorrespondingImport(candidateClassNode.getProperty("id").toString());
			printtypes.put(startPosition, candidateClassNode);
			printTypesMap.put(node.getException().getName().toString(), startPosition);
		}
		variableTypeMap.put(node.getException().getName().toString(), temporaryMap);
		return true;
	}

	//Max parallel
	public void endVisit(SuperConstructorInvocation treeNode)
	{	
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		int startPosition = treeNode.getStartPosition();
		String treeNodeString = treeNode.toString();
		HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
		if(methodReturnTypesMap.containsKey(treeNodeString))
		{
			candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
		}
		else
		{
			candidateAccumulator = HashMultimap.create();
		}
		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(superclassname))
			candidateClassNodes = model.getCandidateClassNodes(superclassname, candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);

		List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
		ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);

		for(NodeJSON candidateClassNode : candidateClassNodes)
		{
			ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, "<init>", candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
			getMethodsInClass.execute(tmicf);
		}
		getMethodsInClass.shutdown();
		while(getMethodsInClass.isTerminated() == false)
		{
		}
		ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
		List<NodeJSON> methodContainerList = Collections.synchronizedList(new ArrayList<NodeJSON>()); 
		for(NodeJSON candidateMethodNode : candidateMethodNodes)
		{
			String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
			if((candidateMethodExactName).equals("<init>"))
			{
				if(matchParams(candidateMethodNode, treeNode.arguments())==true)
				{
					printmethods.put(startPosition, candidateMethodNode);
					ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, methodContainerList, model);
					getMethodContainerExecutor.execute(tmcf);
				}
			}
		}
		getMethodContainerExecutor.shutdown();
		while(getMethodContainerExecutor.isTerminated() == false)
		{

		}
		for(NodeJSON methodContainer : methodContainerList)
		{
			printtypes.put(startPosition, methodContainer);
			candidateAccumulator.put(scopeArray, methodContainer);
		}
		methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
	}

	//Max parallel
	public void endVisit(SuperMethodInvocation treeNode)
	{
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		int startPosition = treeNode.getStartPosition();
		String treeNodeString = treeNode.toString();
		HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
		if(methodReturnTypesMap.containsKey(treeNodeString))
		{
			candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
		}
		else
		{
			candidateAccumulator = HashMultimap.create();
		}
		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(superclassname))
			candidateClassNodes = model.getCandidateClassNodes(superclassname, candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);
		printMethodsMap.put(treeNode.toString(), startPosition);

		List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
		ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);

		for(NodeJSON candidateClassNode : candidateClassNodes)
		{
			ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, treeNode.getName().toString(), candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
			getMethodsInClass.execute(tmicf);
		}
		getMethodsInClass.shutdown();
		while(getMethodsInClass.isTerminated() == false)
		{
		}
		ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
		ExecutorService getMethodReturnExecutor = Executors.newFixedThreadPool(NThreads);
		List<NodeJSON> methodContainerList = Collections.synchronizedList(new ArrayList<NodeJSON>()); 
		for(NodeJSON candidateMethodNode : candidateMethodNodes)
		{
			String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
			if(candidateMethodExactName.equals(treeNode.getName().toString()))
			{
				if(matchParams(candidateMethodNode, treeNode.arguments())==true)
				{
					printmethods.put(startPosition, candidateMethodNode);
					ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, methodContainerList, model);
					getMethodContainerExecutor.execute(tmcf);

					ThreadedMethodReturnFetch tmrf = new ThreadedMethodReturnFetch(candidateMethodNode, methodReturnCache, candidateAccumulator, scopeArray, model, treeNode);
					getMethodReturnExecutor.execute(tmrf);
				}
			}
		}
		getMethodContainerExecutor.shutdown();
		getMethodReturnExecutor.shutdown();
		while(getMethodContainerExecutor.isTerminated() == false || getMethodReturnExecutor.isTerminated() == false)
		{

		}
		if(methodContainerList.isEmpty()==false)
		{
			printtypes.replaceValues(treeNode.getStartPosition(), methodContainerList);
		}
		methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
	}

	//Max parallel
	public void endVisit(final ClassInstanceCreation treeNode)
	{
		ASTNode anon=treeNode.getAnonymousClassDeclaration();
		if(anon!=null)
		{
			anon.accept(new ASTVisitor(){
				public void endVisit(MethodDeclaration md)
				{
					String methodDeclarationName = md.getName().toString();
					int startPosition = md.getStartPosition();
					ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
					if(!isLocalClass(treeNode.getType().toString()))
						candidateClassNodes = model.getCandidateClassNodes(treeNode.getType().toString(), candidateClassNodesCache);
					candidateClassNodes = getNewClassElementsList(candidateClassNodes);

					List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
					ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);

					for(NodeJSON candidateClassNode : candidateClassNodes)
					{
						ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, methodDeclarationName, candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
						getMethodsInClass.execute(tmicf);
					}
					getMethodsInClass.shutdown();
					while(getMethodsInClass.isTerminated() == false)
					{
					}
					ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
					List<NodeJSON> methodContainerList = Collections.synchronizedList(new ArrayList<NodeJSON>()); 
					for(NodeJSON candidateMethodNode : candidateMethodNodes)
					{
						String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
						if(candidateMethodExactName.equals(methodDeclarationName))
						{
							if(matchParams(candidateMethodNode, md.parameters())==true)
							{
								printmethods.put(startPosition, candidateMethodNode);
								printMethodsMap.put(md.toString(), startPosition);
								ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, methodContainerList, model);
								getMethodContainerExecutor.execute(tmcf);

							}
						}
					}
					getMethodContainerExecutor.shutdown();
					while(getMethodContainerExecutor.isTerminated() == false)
					{

					}
					if(methodContainerList.isEmpty()==false)
					{
						for(NodeJSON methodContainer : methodContainerList)
						{
							printtypes.put(startPosition, methodContainer);
							printTypesMap.put(treeNode.toString(), startPosition);
						}
					}
				}
			});
		}
		String treeNodeString = treeNode.toString();
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		int startPosition = treeNode.getType().getStartPosition();
		printMethodsMap.put(treeNodeString, startPosition);
		printTypesMap.put(treeNodeString, startPosition);
		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(treeNode.getType().toString()))
			candidateClassNodes = model.getCandidateClassNodes(treeNode.getType().toString(), candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);
		HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
		if(methodReturnTypesMap.containsKey(treeNodeString))
		{
			candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
		}
		else
		{
			candidateAccumulator = HashMultimap.create();
		}

		List<NodeJSON> candidateMethodNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
		ExecutorService getMethodsInClass = Executors.newFixedThreadPool(NThreads);
		for(NodeJSON candidateClassNode : candidateClassNodes)
		{
			ThreadedMethodsInClassFetch tmicf = new ThreadedMethodsInClassFetch(candidateClassNode, "<init>", candidateMethodNodes, candidateMethodNodesCache, methodContainerCache ,model);
			getMethodsInClass.execute(tmicf);
		}
		getMethodsInClass.shutdown();
		while(getMethodsInClass.isTerminated() == false)
		{
		}

		ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
		List<NodeJSON> methodContainerList = Collections.synchronizedList(new ArrayList<NodeJSON>()); 
		for(NodeJSON candidateMethodNode : candidateMethodNodes)
		{
			String candidateMethodExactName = (String)candidateMethodNode.getProperty("exactName");
			if(candidateMethodExactName.equals("<init>"))
			{
				if(matchParams(candidateMethodNode, treeNode.arguments())==true)
				{
					printmethods.put(startPosition, candidateMethodNode);
					ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(candidateMethodNode, methodContainerCache, methodContainerList, model);
					getMethodContainerExecutor.execute(tmcf);

				}
			}
		}
		getMethodContainerExecutor.shutdown();
		while(getMethodContainerExecutor.isTerminated() == false)
		{

		}
		if(methodContainerList.isEmpty()==false)
		{
			for(NodeJSON methodContainer : methodContainerList)
			{
				printtypes.put(startPosition, methodContainer);
				printTypesMap.put(treeNode.toString(), startPosition);
				candidateAccumulator.put(scopeArray, methodContainer);
			}
		}

		if(treeNode.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT)
		{
			VariableDeclarationFragment lhs = ((VariableDeclarationFragment) treeNode.getParent());

			HashMultimap<ArrayList<Integer>, NodeJSON> tempMap = HashMultimap.create(); 
			tempMap.putAll(getScopeArray(lhs.getParent()), printtypes.get(startPosition));
			variableTypeMap.put(lhs.getName().toString(), tempMap);

		}
		methodReturnTypesMap.put(treeNodeString, candidateAccumulator);
		return;
	}

	//Max parallel
	/*public void endVisit(ClassInstanceCreation treeNode)
	{	
		System.out.println("here -- ClassInstanceCreation");
		System.out.println("endvisit class inst cre 1");
		int startPosition = treeNode.getType().getStartPosition();
		String treeNodeType = treeNode.getType().toString();
		System.out.println(treeNodeType);
		ArrayList<NodeJSON> candidateClassNodes = model.getCandidateClassNodes(treeNodeType, candidateClassNodesCache);
		candidateClassNodes = getNewCeList(candidateClassNodes);
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		String treeNodeString = treeNode.toString();
		HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator = null;
		if(methodReturnTypesMap.containsKey(treeNodeString))
		{
			candidateAccumulator = methodReturnTypesMap.get(treeNodeString);
		}
		else
		{
			candidateAccumulator = HashMultimap.create();
		}
		for(NodeJSON candidateClassNode : candidateClassNodes)
		{
			System.out.println("here");
			printTypesMap.put(treeNode.toString(), startPosition);
			printtypes.put(startPosition, candidateClassNode);
			candidateAccumulator.put(scopeArray, candidateClassNode);
		}
		methodReturnTypesMap.put(treeNodeString, candidateAccumulator);

	}*/

	//Max parallel
	public boolean visit(CastExpression node)
	{

		Assignment assNode = null;
		VariableDeclarationFragment varNode = null;
		//find corresponding assignment statement
		ASTNode temp = node;
		while(temp != null && temp.getNodeType() != ASTNode.ASSIGNMENT && temp.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
		{
			temp = temp.getParent();
		}
		if(temp!=null)
		{
			if(temp.getNodeType() == ASTNode.ASSIGNMENT )
				assNode = (Assignment) temp;
			else if(temp.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
				varNode = (VariableDeclarationFragment) temp;
		}

		ArrayList<NodeJSON> candidateClassNodes = new ArrayList<NodeJSON>();
		if(!isLocalClass(node.getType().toString()))
			candidateClassNodes = model.getCandidateClassNodes(node.getType().toString(), candidateClassNodesCache);
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);

		HashMultimap<ArrayList<Integer>, NodeJSON> temp1= null;
		HashMultimap<ArrayList<Integer>, NodeJSON> temp2= null;
		HashMultimap<ArrayList<Integer>, NodeJSON> temp3 = HashMultimap.create();
		ArrayList<Integer> scopeArray = getScopeArray(node);
		if(variableTypeMap.containsKey(node.toString()))
		{
			temp1 = variableTypeMap.get(node.toString());
		}
		else
		{
			temp1 = HashMultimap.create();
		}
		if(variableTypeMap.containsKey("("+node.toString()+")"))
		{
			temp2 = variableTypeMap.get("("+node.toString()+")");
		}
		else
		{
			temp2 = HashMultimap.create();
		}
		for(NodeJSON candidateClassNode : candidateClassNodes)
		{
			temp1.put(scopeArray, candidateClassNode);
			if(candidateClassNodes.size() < tolerance)
				addCorrespondingImport(candidateClassNode.getProperty("id").toString());
			printtypes.put(node.getType().getStartPosition(), candidateClassNode);
			temp2.put(scopeArray, candidateClassNode);
			temp3.put(scopeArray, candidateClassNode);
		}
		variableTypeMap.put(node.toString(), temp1);
		variableTypeMap.put("("+node.toString()+")", temp2);
		if(assNode != null)
		{
			if(assNode.getLeftHandSide().getNodeType() == ASTNode.THIS_EXPRESSION)
			{
				ThisExpression thisExp = (ThisExpression) assNode.getLeftHandSide();
				String name = thisExp.getQualifier().toString();
			}
			methodReturnTypesMap.put(assNode.getRightHandSide().toString(), temp3);
			
		}
		else if(varNode != null)
			methodReturnTypesMap.put(varNode.getInitializer().toString(), temp3);
		return true;
	}

	//Max parallel
	public void endVisit(Assignment node)
	{
		String lhs,rhs;
		lhs=node.getLeftHandSide().toString();
		rhs=node.getRightHandSide().toString();

		/*if(node.getRightHandSide().getNodeType() == ASTNode.CAST_EXPRESSION)
			return;*/
		if(methodReturnTypesMap.containsKey(rhs))
		{
			if(!variableTypeMap.containsKey(lhs))
			{
				if(node.getLeftHandSide().getNodeType() == ASTNode.METHOD_INVOCATION)
				{
					methodReturnTypesMap.put(lhs, methodReturnTypesMap.get(rhs));
				}
				else
				{
					HashMultimap<ArrayList<Integer>, NodeJSON> tempMap = methodReturnTypesMap.get(rhs);

					ArrayList<Integer> scopeArray = getScopeArray(node.getRightHandSide());
					ArrayList<Integer> rightScopeArray = getNodeSet(tempMap, scopeArray);
					Set<NodeJSON> candidateSet = tempMap.get(rightScopeArray);
					ArrayList<Integer> fieldScope = new ArrayList<Integer>();
					fieldScope.add(0);
					HashMultimap<ArrayList<Integer>, NodeJSON> newTempMap = HashMultimap.create();
					newTempMap.putAll(fieldScope, candidateSet);

					if(node.getLeftHandSide().getNodeType() == ASTNode.SIMPLE_NAME)
					{
						variableTypeMap.put(lhs, newTempMap);
					}
					else if(node.getLeftHandSide().getNodeType() == ASTNode.FIELD_ACCESS)
					{
						FieldAccess leftNode = (FieldAccess) node.getLeftHandSide(); 

						if(leftNode.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION)
						{
							String leftName = leftNode.getName().toString();
							variableTypeMap.put(leftName, newTempMap);
							printTypesMap.put(leftName, node.getLeftHandSide().getStartPosition());
						}
						else
						{
							methodReturnTypesMap.put(lhs, methodReturnTypesMap.get(rhs));
						}
					}
					else
					{
						methodReturnTypesMap.put(lhs, methodReturnTypesMap.get(rhs));
						methodReturnTypesMap.put(lhs, methodReturnTypesMap.get(rhs));
					}
				}
			}
			else
			{	
				int flag=0;
				Set<NodeJSON> temp = new HashSet<NodeJSON>();
				HashMultimap<ArrayList<Integer>, NodeJSON> celist_temp = variableTypeMap.get(lhs);
				ArrayList<Integer> scopeArray = getNodeSet(celist_temp, getScopeArray(node));
				if(scopeArray!=null)
				{
					Set<NodeJSON> localTypes = celist_temp.get(scopeArray);
					for(NodeJSON ce : localTypes)
					{
						if(methodReturnTypesMap.get(rhs).values().contains(ce))
						{
							flag=1;
							temp.add(ce);
						}
					}
				}
				if(flag==1)
				{
					variableTypeMap.get(lhs).replaceValues(scopeArray,temp);
				}

			}
		}
	}

	//Max parallel
	public boolean visit(ImportDeclaration node)
	{

		String importStatement = node.getName().getFullyQualifiedName();
		if(importStatement.endsWith(".*"))
		{
			importStatement= importStatement.substring(0, importStatement.length()-2);
		}
		importList.add(importStatement);
		return true;
	}

	//Max parallel
	public JSONObject printJson()
	{
		checkForNull();

		JSONObject main_json=new JSONObject();

		//Collections.sort(printtypes, printtypes.keySet());
		for(Integer key:printtypes.keySet())
		{
			int flag=0;
			String cname=null;
			List<String> namelist = new ArrayList<String>();
			for(NodeJSON type_name:printtypes.get(key))
			{
				//if(!isPrimitive(type_name.getProperty("id")))
				{
					String nameOfClass = (String)type_name.getProperty("id");
					namelist.add("\""+nameOfClass+"\"");
					if(flag==0)
					{
						cname=(String) type_name.getProperty("exactName");
						flag=1;
					}
				}
			}
			
			if(namelist.isEmpty()==false)
			{
				JSONObject json = new JSONObject();
				json.accumulate("line_number",Integer.toString(cu.getLineNumber(key)-cutype));
				json.accumulate("precision", Integer.toString(namelist.size()));
				json.accumulate("name",cname);
				json.accumulate("elements",namelist);
				json.accumulate("type","api_type");
				json.accumulate("character", Integer.toString(key));
				main_json.accumulate("api_elements", json);
			}

		}
		for(Integer key:printmethods.keySet())
		{
			List<String> namelist = new ArrayList<String>();
			String mname=null;
			for(NodeJSON method_name:printmethods.get(key))
			{
				String nameOfMethod = (String)method_name.getProperty("id");
				namelist.add("\""+nameOfMethod+"\"");
				mname=(String) method_name.getProperty("exactName");
			}
			if(namelist.isEmpty()==false)
			{
				JSONObject json = new JSONObject();
				json.accumulate("line_number",Integer.toString(cu.getLineNumber(key)-cutype));
				json.accumulate("precision", Integer.toString(namelist.size()));
				json.accumulate("name",mname);
				json.accumulate("elements",namelist);
				json.accumulate("type","api_method");
				json.accumulate("character", Integer.toString(key));
				main_json.accumulate("api_elements", json);
			}
		}
		if(main_json.isNull("api_elements"))
		{
			String emptyJSON = "{\"api_elements\": [{ \"precision\": \"\",\"name\": \"\",\"line_number\": \"\",\"type\": \"\",\"elements\": \"\"}]}" ;
			JSONObject ret = new JSONObject();
			try 
			{
				ret = new JSONObject(emptyJSON);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			return(ret);
		}
		else
		{
			return(main_json);
		}
	}

	//Max parallel
	public void checkForNull()
	{
		printtypes.removeAll(null);
		printmethods.removeAll(null);
		for(Integer key : printtypes.keySet())
		{
			for(NodeJSON type_name:printtypes.get(key))
			{
				if(type_name==null)
					printtypes.remove(key, type_name);
			}
		}
		for(Integer key : printmethods.keySet())
		{
			for(NodeJSON method_name:printmethods.get(key))
			{
				if(method_name==null)
					printmethods.remove(key, method_name);
			}
		}
	}


}