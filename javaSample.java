 public static IR.Program gen(Ast.Program n) throws Exception {
    Ast.ClassDecl[] classes = topoSort(n.classes);
    ClassInfo cinfo;
    for (Ast.ClassDecl c: classes) {
      cinfo = createClassInfo(c);
      classInfos.put(c.nm, cinfo);
    }
    List<IR.Data> allData = new ArrayList<IR.Data>();
    List<IR.Func> allFuncs = new ArrayList<IR.Func>();
    for (Ast.ClassDecl c: classes) {
      cinfo = classInfos.get(c.nm);
      IR.Data data = genData(c, cinfo);
      List<IR.Func> funcs = gen(c, cinfo);
      if (data != null)
	allData.add(data);
      allFuncs.addAll(funcs);
    }
    return new IR.Program(allData, allFuncs);
  }

  // ClassDecl ---
  // String nm, pnm;
  // VarDecl[] flds;
  // MethodDecl[] mthds;
  //

  // 1. Generate static data
  //
  // Codegen Guideline: 
  //   1.1 For each method in class's vtable, construct a global label of form
  //       "<base class name>_<method name>" and save it in an IR.Global node
  //   1.2 Assemble the list of IR.Global nodes into an IR.Data node with a
  //       global label "class_<class name>" 
  //
  // (Skip this method if class is the static class containing "main".)
  //
  static IR.Data genData(Ast.ClassDecl n, ClassInfo cinfo) throws Exception {
		if(!cinfo.vtable.contains("main"))
		{	
			int i = 0;
			List<IR.Global> globHolder = new ArrayList<IR.Global>();
			for(String methName: cinfo.vtable)
			{
				ClassInfo baseClass = cinfo.methodBaseClass(methName);
				IR.Global globLabel = new IR.Global(baseClass.name + "_" + methName);
				globHolder.add(globLabel);
				i = i + 8;
			}
			IR.Data staticData= new IR.Data(new IR.Global("class" + "_" +  n.nm ), i, globHolder);
			return staticData;
		}
		return null;
    //    ... need code


  }

  // 2. Generate code
  //
  // Codegen Guideline: 
  //   Straightforward -- generate a IR.Func for each mthdDecl.
  //
  static List<IR.Func> gen(Ast.ClassDecl n, ClassInfo cinfo) throws Exception {
		List<IR.Func> fun = new ArrayList<IR.Func>();
		for(Ast.MethodDecl m: n.mthds)
		{
			fun.add(gen(m, cinfo));
		}
		return fun;
    //    ... need code
  }

  // MethodDecl ---
  // Type t;
  // String nm;
  // Param[] params;
  // VarDecl[] vars;
  // Stmt[] stmts;
  //
  // Codegen Guideline: 
  // 1. Construct a global label of form "<base class name>_<method name>"
  // 2. Add "obj" into the params list as the 0th item
  // (Skip these two steps if method is "main".)
  // 3. Create an Env() containing all params and all local vars 
  // 4. Generate IR code for all statements
  // 5. Return an IR.Func with the above
  //
  static IR.Func gen(Ast.MethodDecl n, ClassInfo cinfo) throws Exception {
		IR.Temp.reset();
		List<IR.Inst> code = new ArrayList<IR.Inst>();
		code.add(new IR.LabelDec("Begin"));
		List<Ast.Param> par = new ArrayList<Ast.Param>(Arrays.asList(n.params));
		IR.Global lab = null;
		IR.Func fun = null;
		if(!n.nm.equals("main"))
		{
			ClassInfo binfo = cinfo.methodBaseClass(n.nm);
			lab = new IR.Global(binfo.name + "_" + n.nm);
			par.add(0, new Ast.Param(new Ast.ObjType(cinfo.name), "obj"));
		}
		List<String> parNames = new ArrayList<String>();
		List<String> varNames = new ArrayList<String>();
		Env env = new Env();
		for(Ast.Param p: par)
		{
			env.put(p.nm, p.t);
			parNames.add(p.nm);
		}
		for(Ast.VarDecl v: n.vars)
		{
			env.put(v.nm, v.t);
			varNames.add(v.nm);
			List<IR.Inst> c = gen(v, cinfo, env);
               if(c != null)
                  code.addAll(c);
		}
		
		for(Ast.Stmt s: n.stmts)
		{
			code.addAll(gen(s, cinfo, env));
		}
		if(n.t == null)
		{
			code.add(new IR.Return());
		}
		code.add(new IR.LabelDec("End"));
		if(lab != null)
			fun = new IR.Func(lab.name, parNames, varNames, code);
		else
			fun = new IR.Func(n.nm, parNames, varNames, code);

		
		return fun;
    //    ... need code

  } 

  // VarDecl ---
  // Type t;
  // String nm;
  // Exp init;
  //
  // Codegen Guideline: 
  // 1. If init exp exists, generate IR code for it and assign result to var
  // 2. Return generated code (or null if none)
  //
  private static List<IR.Inst> gen(Ast.VarDecl n, ClassInfo cinfo, Env env) throws Exception {
		List<IR.Inst>  retCode = null;
		if(n.init != null)
		{
			CodePack p = gen(n.init, cinfo, env);
			retCode= new ArrayList<IR.Inst>();
			if(p.code != null)
				retCode.addAll(p.code);
			IR.Move temp = new IR.Move(new IR.Id(n.nm),p.src);
			retCode.add(temp);
		}
		return retCode;
    //    ... need code
  }

  // STATEMENTS

  // Dispatch a generic call to a specific Stmt routine
  // 
  static List<IR.Inst> gen(Ast.Stmt n, ClassInfo cinfo, Env env) throws Exception {
    if (n instanceof Ast.Block) 	return gen((Ast.Block) n, cinfo, env);
    else if (n instanceof Ast.Assign)   return gen((Ast.Assign) n, cinfo, env);
    else if (n instanceof Ast.CallStmt) return gen((Ast.CallStmt) n, cinfo, env);
    else if (n instanceof Ast.If) 	return gen((Ast.If) n, cinfo, env);
    else if (n instanceof Ast.While)    return gen((Ast.While) n, cinfo, env);
    else if (n instanceof Ast.Print)    return gen((Ast.Print) n, cinfo, env);
    else if (n instanceof Ast.Return)   return gen((Ast.Return) n, cinfo, env);
    throw new GenException("Illegal Ast Stmt: " + n);
  }

  // Block ---
  // Stmt[] stmts;
  //
  static List<IR.Inst> gen(Ast.Block n, ClassInfo cinfo, Env env) throws Exception {
		List<IR.Inst> retCode = new ArrayList<IR.Inst>();
		for(Ast.Stmt s: n.stmts)
		{
			retCode.addAll(gen(s, cinfo, env));
		}
		return retCode;
    //    ... need code
  }

  // Assign ---
  // Exp lhs, rhs;
  //
  // Codegen Guideline: 
  // 1. call gen() on rhs
  // 2. if lhs is ID, check against Env to see if it's a local var or a param;
  //    if yes, generate an IR.Move instruction
  // 3. otherwise, call genAddr() on lhs, and generate an IR.Store instruction
  //
  static List<IR.Inst> gen(Ast.Assign n, ClassInfo cinfo, Env env) throws Exception {
		List<IR.Inst> retCode= new ArrayList<IR.Inst>();
		CodePack p = gen(n.rhs, cinfo, env);
		if(p.code != null)
			retCode.addAll(p.code);
		if(n.lhs instanceof Ast.Id)
		{
			if (env.get(((Ast.Id)(n.lhs)).nm) != null)
			{
				IR.Move tempMove = new IR.Move(new IR.Id(((Ast.Id)(n.lhs)).nm), p.src);
				retCode.add(tempMove);
				return retCode;
			}
			AddrPack  a = genAddr(new Ast.Field(new Ast.This(), ((Ast.Id)(n.lhs)).nm),cinfo, env);
			retCode.addAll(a.code);
			IR.Store tempStore = new IR.Store(gen(a.type), a.addr, p.src);
			retCode.add(tempStore);
			return retCode;
		}
		AddrPack  a = genAddr(n.lhs, cinfo, env);
		retCode.addAll(a.code);
		IR.Store tempStore = new IR.Store(gen(a.type), a.addr, p.src);
		retCode.add(tempStore);
		return retCode;

    //    ... need code
  }

  // CallStmt ---
  // Exp obj; 
  // String nm;
  // Exp[] args;
  //
  //
  static List<IR.Inst> gen(Ast.CallStmt n, ClassInfo cinfo, Env env) throws Exception {
    if (n.obj != null) {
      CodePack p = handleCall(n.obj, n.nm, n.args, cinfo, env, false);
      return p.code;
    }
    throw new GenException("In CallStmt, obj is null " + n);  
  }

  // handleCall
  // ----------
  // Common routine for Call and CallStmt nodes
  //
  // Codegen Guideline: 
  // 1. Invoke gen() on obj, which returns obj's storage address (and type and code)
  // 2. With type info in the returning CodePack, figure out obj's base class
  // 3. Access the base class's ClassInfo rec to get the method's offset in vtable 
  // 4. Add obj's as the 0th argument to the args list
  // 5. Generate an IR.Load to get the class descriptor from obj's storage
  // 6. Generate another IR.Load to get the method's global label
  // 7. If retFlag is set, prepare a temp for receiving return value; also figure
  //    out return value's type (through method's decl in ClassInfo rec)
  // 8. Generate an indirect call with the global label
  //
  static CodePack handleCall(Ast.Exp obj, String name, Ast.Exp[] args, ClassInfo cinfo, Env env, boolean retFlag) throws Exception {
		CodePack objPack = gen(obj, cinfo, env);
		List<IR.Inst> retCode = new ArrayList<IR.Inst>();
		if(objPack.code != null)
			retCode.addAll(objPack.code);
		Ast.Type objType = objPack.type;
		ClassInfo binfo = classInfos.get(((Ast.ObjType)objType).nm);
		int offset = binfo.methodOffset(name);
		List<Ast.Exp> argList = new ArrayList<Ast.Exp>(Arrays.asList(args));
		argList.add(0, obj);
		IR.Temp t = new IR.Temp();
		IR.Temp t1 = new IR.Temp();
		IR.Load load1= new IR.Load(gen(objType), t, new IR.Addr(objPack.src, 0));
		IR.Load load2 = new IR.Load(gen(objType), t1, new IR.Addr(t, offset));
		retCode.add(load1);
		retCode.add(load2);
		IR.Temp t2 = null;
		List<IR.Src> srcs= new ArrayList<IR.Src>();
		for(Ast.Exp e: argList)
		{
			srcs.add(gen(e, cinfo, env).src);
		}
		if(retFlag)
		{
			t2 = new IR.Temp();
			Ast.Type type = binfo.methodType(name);
			IR.Call retCall = new IR.Call(t1, true, srcs, t2);
			retCode.add(retCall);
			
			return new CodePack(type, t2, retCode);
		}
		Ast.Type type = binfo.methodType(name);
		IR.Call call = new IR.Call(t1, true, srcs, t2);
		retCode.add(call);
		return new CodePack(type, objPack.src, retCode);
    //    ... need code


  }

  // If ---
  // Exp cond;
  // Stmt s1, s2;
  //
  // (See class notes.)
  //
  static List<IR.Inst> gen(Ast.If n, ClassInfo cinfo, Env env) throws Exception {
		CodePack p = gen(n.cond, cinfo, env);
		List<IR.Inst> code = new ArrayList<IR.Inst>();
		if(p.code != null)
			code.addAll(p.code);
		IR.Label L1 = new IR.Label();
		IR.Label L2 = null;
		if(n.s2 != null)
			L2 = new IR.Label();
		code.add(new IR.CJump(IR.RelOP.EQ, p.src, new IR.BoolLit(false), L1));
		code.addAll(gen(n.s1, cinfo, env));
		if(L2 != null)
			code.add(new IR.Jump(L2));
		code.add(new IR.LabelDec(L1.toString()));
		if(n.s2 != null)
		{
			code.addAll(gen(n.s2, cinfo, env));
		}
		if(L2 != null)
			code.add(new IR.LabelDec(L2.toString()));
		return code;
    //    ... need code
  }

  // While ---
  // Exp cond;
  // Stmt s;
  //
  // (See class notes.)
  //
  static List<IR.Inst> gen(Ast.While n, ClassInfo cinfo, Env env) throws Exception {
		CodePack p = gen(n.cond, cinfo, env);
		IR.Label L1 = new IR.Label();
		IR.Label L2 = new IR.Label();
		List<IR.Inst> retCode = new ArrayList<IR.Inst>();
		retCode.add(new IR.LabelDec(L1.toString()));
		if(p.code != null)
			retCode.addAll(p.code);
		retCode.add(new IR.CJump(IR.RelOP.EQ, p.src, new IR.BoolLit(false), L2));
		retCode.addAll(gen(n.s, cinfo, env));
		retCode.add(new IR.Jump(L1));
		retCode.add(new IR.LabelDec(L2.toString()));
		return retCode;

    //    ... need code


  }
  
  // Print ---
  // Exp arg;
  //
  // Codegen Guideline: 
  // 1. If arg is null, generate an IR.Call with "print"
  // 2. If arg is StrLit, generate an IR.Call with "printStr"
  // 3. Otherwise, generate IR code for arg, and use its type info
  //    to decide which of the two functions, "printInt" and "printBool",
  //    to call
  //
  static List<IR.Inst> gen(Ast.Print n, ClassInfo cinfo, Env env) throws Exception {

	List<IR.Src> srcs = new ArrayList<IR.Src>();
	List<IR.Inst> retCode = new ArrayList<IR.Inst>();
	if(n.arg == null)
	{
		retCode.add(new IR.Call(new IR.Global("print"), false, srcs, null));
	}
	else if(n.arg instanceof Ast.StrLit)
	{
		srcs.add(gen(((Ast.StrLit)(n.arg))).src);
		retCode.add(new IR.Call(new IR.Global("printStr"), false, srcs, null));
	}
	else
	{
		CodePack p = gen(n.arg, cinfo, env);
		if(p.code != null)
			retCode.addAll(p.code);
		if(p.type instanceof Ast.IntType)
		{
			srcs.add(p.src);
			retCode.add(new IR.Call(new IR.Global("printInt"), false, srcs, null));
		}
		else if(p.type instanceof Ast.BoolType)
		{
			srcs.add(p.src);
			retCode.add(new IR.Call(new IR.Global("printBool"), false, srcs, null));
		}
	}
	return retCode;
    //    ... need code
  }

  // Return ---  
  // Exp val;
  //
  // Codegen Guideline: 
  // 1. If val is non-null, generate IR code for it, and generate an IR.Return
  //    with its value
  // 2. Otherwise, generate an IR.Return with no value
  //
  static List<IR.Inst> gen(Ast.Return n, ClassInfo cinfo, Env env) throws Exception {
		List<IR.Inst> retCode = new ArrayList<IR.Inst>();
		if(n.val != null)
		{
			CodePack p = gen(n.val, cinfo, env);
			if(p.code != null)
				retCode.addAll(p.code);
			retCode.add(new IR.Return(p.src));
			return retCode;
		}
		else
			retCode.add(new IR.Return());
			return retCode;

    //    ... need code


  }

  // EXPRESSIONS

  // 1. Dispatch a generic gen call to a specific gen routine
  //
  static CodePack gen(Ast.Exp n, ClassInfo cinfo, Env env) throws Exception {
    if (n instanceof Ast.Call)    return gen((Ast.Call) n, cinfo, env);
    if (n instanceof Ast.NewObj)  return gen((Ast.NewObj) n, cinfo, env);
    if (n instanceof Ast.Field)   return gen((Ast.Field) n, cinfo, env);
    if (n instanceof Ast.Id)	  return gen((Ast.Id) n, cinfo, env);
    if (n instanceof Ast.This)    return gen((Ast.This) n, cinfo);
    if (n instanceof Ast.IntLit)  return gen((Ast.IntLit) n);
    if (n instanceof Ast.BoolLit) return gen((Ast.BoolLit) n);
    if (n instanceof Ast.StrLit)  return gen((Ast.StrLit) n);
    throw new GenException("Exp node not supported in this codegen: " + n);
  }

  // 2. Dispatch a generic genAddr call to a specific genAddr routine
  //    (Only one LHS Exp needs to be implemented for this assignment)
  //
  static AddrPack genAddr(Ast.Exp n, ClassInfo cinfo, Env env) throws Exception {
    if (n instanceof Ast.Field) return genAddr((Ast.Field) n, cinfo, env);
    throw new GenException(" LHS Exp node not supported in this codegen: " + n);
  }

  // Call ---
  // Exp obj; 
  // String nm;
  // Exp[] args;
  //
  static CodePack gen(Ast.Call n, ClassInfo cinfo, Env env) throws Exception {
    if (n.obj != null)
	return handleCall(n.obj, n.nm, n.args, cinfo, env, true);
    throw new GenException("In Call, obj is null: " + n);  
  } 
  
  // NewObj ---
  // String cn;
  // Exp[] args; (ignored)
  //
  // Codegen Guideline: 
  //  1. Use class name to find the corresponding ClassInfo record
  //  2. Find the class's type and object size from the ClassInfo record
  //  3. Cosntruct a malloc call to allocate space for the object
  //  4. Store a pointer to the class's descriptor into the first slot of
  //     the allocated space
  //
  static CodePack gen(Ast.NewObj n, ClassInfo cinfo, Env env) throws Exception {
		List<IR.Inst> retCode = new ArrayList<IR.Inst>();
		List<IR.Src> srcs = new ArrayList<IR.Src>();
		ClassInfo info = classInfos.get(n.nm);
		int size = info.objSize;
		srcs.add(new IR.IntLit(size));
		IR.Type type = IR.Type.PTR;
		IR.Temp t1 = new IR.Temp();
		retCode.add(new IR.Call(new IR.Global("malloc"), false, srcs, t1));
		retCode.add(new IR.Store(type, new IR.Addr(t1), new IR.Global("class_" + n.nm)));
		return new CodePack(new Ast.ObjType(n.nm), t1, retCode); 
		
		
    //    ... need code


  }
  
  // Field ---
  // Exp obj; 
  // String nm;
  //

  // 1. gen()
  //
  // Codegen Guideline: 
  //   1.1 Call genAddr to generate field variable's address
  //   1.2 Add an IR.Load to get its value
  //
  static CodePack gen(Ast.Field n, ClassInfo cinfo, Env env) throws Exception {
		
		List<IR.Inst> ret = new ArrayList<IR.Inst>();
		AddrPack ad = genAddr(n, cinfo, env);
		IR.Temp t = new IR.Temp();
		ret.add(new IR.Load(gen(ad.type),  t, ad.addr));
		return new CodePack(ad.type, t, ret);

    //    ... need code


  }
  
  // 2. genAddr()
  //
  // Codegen Guideline: 
  //   2.1 Call gen() on the obj component
  //   2.2 Use the type info to figure out obj's base class
  //   2.3 Access base class's ClassInfo rec to get field variable's offset
  //   2.4 Generate an IR.Addr based on the offset
  //
  static AddrPack genAddr(Ast.Field n, ClassInfo cinfo, Env env) throws Exception {
		CodePack p = gen(n.obj, cinfo, env);
		List<IR.Inst> retCode = new ArrayList<IR.Inst>();
		if(p.code != null)
			retCode.addAll(p.code);
		Ast.Type objType = p.type;
		ClassInfo baseClass = classInfos.get(((Ast.ObjType)objType).nm);
		int offset = baseClass.fieldOffset(n.nm);
		IR.Addr address = new IR.Addr(new IR.Id(p.src.toString()), offset);
		return new AddrPack(baseClass.fieldType(n.nm), address, retCode);
    //    ... need code


  }
  
  // Id ---
  // String nm;
  //
  // Codegen Guideline: 
  //  1. Check to see if the Id is in the env.
  //  2. If so, it means it is a local variable or a parameter. Just return
  //     a CodePack containing the Id.
  //  3. Otherwise, the Id is an instance variable. Convert it into an
  //     Ast.Field node with Ast.This() as its obj, and invoke the gen routine 
  //     on this new node
  //
  static CodePack gen(Ast.Id n, ClassInfo cinfo, Env env) throws Exception {

		if(env.get(n.nm) != null)
		{
			return new CodePack(env.get(n.nm), new IR.Id(n.nm), null);
		}
		else
		{
			return gen(new Ast.Field(new Ast.This(), n.nm),cinfo, env);
		}
    //    ... need code
  }

  // This ---
  //
  static CodePack gen(Ast.This n, ClassInfo cinfo) throws Exception {
    return new CodePack(new Ast.ObjType(cinfo.name), thisObj);
  }

  // IntLit ---
  // int i;
  //
  static CodePack gen(Ast.IntLit n) throws Exception {
    return  new CodePack(AstIntType, new IR.IntLit(n.i));
  }

  // BoolLit ---
  // boolean b;
  //
  static CodePack gen(Ast.BoolLit n) throws Exception {
    return  new CodePack(AstBoolType, n.b ? IR.TRUE : IR.FALSE);
  }

  // StrLit ---
  // String s;
  //
  static CodePack gen(Ast.StrLit n) throws Exception {
    return new CodePack(null, new IR.StrLit(n.s));
  }

  // Type mapping (AST -> IR)
  //
  static IR.Type gen(Ast.Type n) throws Exception {
    if (n == null)                  return null;
    if (n instanceof Ast.IntType)   return IR.Type.INT;
    if (n instanceof Ast.BoolType)  return IR.Type.BOOL;
    if (n instanceof Ast.ObjType)   return IR.Type.PTR;
    throw new GenException("Invalid Ast type: " + n);
  }

}
